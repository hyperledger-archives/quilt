package org.interledger.stream.pay.trackers;

import static org.interledger.stream.pay.trackers.MaxPacketAmountTracker.MaxPacketState.ImpreciseMax;
import static org.interledger.stream.pay.trackers.MaxPacketAmountTracker.MaxPacketState.PreciseMax;
import static org.interledger.stream.pay.trackers.MaxPacketAmountTracker.MaxPacketState.UnknownMax;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedLong;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import org.immutables.value.Value;
import org.immutables.value.Value.Immutable;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.fluent.FluentCompareTo;
import org.interledger.core.fluent.FluentUnsignedLong;
import org.interledger.core.fluent.InterledgerPacketUtils;
import org.interledger.core.fluent.Ratio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks the Max Packet Amount for a given payment path.
 */
public class MaxPacketAmountTracker {

  private static final Logger LOGGER = LoggerFactory.getLogger(MaxPacketAmountTracker.class);

  /**
   * An object that allows for fine-grained interpretation of the max-packet amount for a path. This value can differ
   * slightly from the verified path-capacity because the verified amount maybe be less-than the discovered max-packet
   * amount. For example, imagine an exchange-rate probe on a path with a maxPacketAmount of 5 that sends two packets,
   * one for 10 and one for 1. In this example, the maxPacketAmount will be 9 (imprecise) whereas the verified
   * path-capacity will be 1.
   */
  private final AtomicReference<MaxPacketAmount> maxPacketAmountRef = new AtomicReference<>(
    MaxPacketAmount.unknownMax());

  /**
   * The greatest amount that the recipient has acknowledged to have received. Note: this is always reduced so it's
   * never greater than the max packet amount.
   */
  private final AtomicReference<UnsignedLong> verifiedPathCapacityRef = new AtomicReference<>(UnsignedLong.ZERO);

  /**
   * Is the max packet amount 0 and impossible to send value over this path?
   */
  private final AtomicBoolean noCapacityAvailableRef = new AtomicBoolean();

  /**
   * Decrease the path max-packet amount in response to an F08 ILP rejection.
   *
   * @param interledgerRejectPacket A {@link InterledgerRejectPacket} that contains a data payload with F08 metadata in
   *                                {@link InterledgerRejectPacket#typedData()}.
   * @param sourceAmount            An {@link UnsignedLong} with the original amount sent in the STREAM request.
   */
  public synchronized void reduceMaxPacketAmount(
    final InterledgerRejectPacket interledgerRejectPacket, final UnsignedLong sourceAmount
  ) {
    Objects.requireNonNull(interledgerRejectPacket);
    Objects.requireNonNull(sourceAmount);

    InterledgerPacketUtils.getAmountTooLargeErrorData(interledgerRejectPacket)
      .map(amountTooLargeErrorData -> { // <-- Logging only
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Handling F08 Reject packet. amountTooLargeErrorData={}", amountTooLargeErrorData);
        }
        return amountTooLargeErrorData; // <-- Return for further processing below.
      })
      .map(amountTooLargeErrorData -> {
        final UnsignedLong totalReceivedByRemote = amountTooLargeErrorData.receivedAmount();
        final UnsignedLong remoteMaximum = amountTooLargeErrorData.maximumAmount();

        // F08 is invalid if the remote received less than their own maximum! This check ensures that remoteReceived
        // is always > 0
        if (FluentCompareTo.is(totalReceivedByRemote).greaterThan(remoteMaximum)) {
          return Optional.<MaxPacketAmount>empty();
        }

        // Convert remote max packet amount into source units
        final Ratio exchangeRate = Ratio.from(sourceAmount, totalReceivedByRemote);
        // newMaxAmount <= sourceAmount because remoteMaximum / totalReceivedByRemote is < 1
        final UnsignedLong newMaxAmount = FluentUnsignedLong.of(remoteMaximum).timesFloor(exchangeRate).getValue();
        return Optional.<MaxPacketAmount>of(
          MaxPacketAmount.builder().maxPacketState(PreciseMax).value(newMaxAmount).build()
        );
      })
      // There was no amountTooLargeErrorData that could be found, so the only thing we can infer is that the amount
      // sent was too high. So, simply reduce by ONE because this is the next value that _might_ be possible for sending
      // (but more probing would allow us to know).
      .orElseGet(() -> {
        final MaxPacketAmount newMaxPacketAmount = MaxPacketAmount.builder()
          .maxPacketState(ImpreciseMax)
          .value(sourceAmount.minus(UnsignedLong.ONE))
          .build();
        return Optional.of(newMaxPacketAmount);
      })
      .ifPresent(newMaxPacketAmount -> {

        if (newMaxPacketAmount.value().isPresent() &&
          FluentUnsignedLong.of(newMaxPacketAmount.value().get()).isNotPositive()) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Ending payment: max packet amount is 0, cannot send over path");
          }
          this.noCapacityAvailableRef.set(true);
          return;
        }

        final MaxPacketAmount originalMaxPacketStateSnapshot = this.getMaxPacketAmount();

        switch (originalMaxPacketStateSnapshot.maxPacketState()) {
          case UnknownMax: {
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("Setting initial maxPacketAmount to {}", newMaxPacketAmount);
            }
            this.maxPacketAmountRef.set(newMaxPacketAmount);
            this.adjustPathCapacity(this.verifiedPathCapacityRef.get());
            break;
          }
          case ImpreciseMax: {
            if (originalMaxPacketStateSnapshot.value().isPresent() && newMaxPacketAmount.value().isPresent() &&
              FluentCompareTo.is(newMaxPacketAmount.value().get())
                .lessThan(originalMaxPacketStateSnapshot.value().get())
            ) {
              if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                  "Reducing max packet amount from {} to {}",
                  originalMaxPacketStateSnapshot.value().get(), newMaxPacketAmount.value().get()
                );
              }
              this.maxPacketAmountRef.set(newMaxPacketAmount);
              this.adjustPathCapacity(this.verifiedPathCapacityRef.get());
            }
            break;
          }
          case PreciseMax: { // <--  Ignore F08s that don't lower the max packet amount.
            break;
          }
          default: {
            // Do nothing.
            throw new IllegalStateException(
              "Unhandled MaxPacketState: " + originalMaxPacketStateSnapshot.maxPacketState()
            );
          }
        }

      });
  }

  /**
   * Increase the amount acknowledged by the recipient (i.e., the "path capacity"), which indicates the path is capable
   * of sending packets of at least that amount.
   *
   * @param ackAmount An {@link UnsignedLong} representing an amount acknowledged by the other party to this stream
   *                  payment.
   */
  public synchronized void adjustPathCapacity(final UnsignedLong ackAmount) {
    Objects.requireNonNull(ackAmount);

    final UnsignedLong originalVerifiedPathCapacitySnapshot = this.verifiedPathCapacityRef.get();
    final MaxPacketAmount maxPacketAmountSnapshot = this.maxPacketAmountRef.get();
    final UnsignedLong maxPacketAmountValue = maxPacketAmountSnapshot.maxPacketState() == UnknownMax ?
      UnsignedLong.MAX_VALUE : maxPacketAmountSnapshot.value().get(); // <-- Will always work due to PreCond

    final UnsignedLong newPathCapacity = FluentUnsignedLong.of(originalVerifiedPathCapacitySnapshot)
      .orGreater(ackAmount)
      .orLesser(maxPacketAmountValue)
      .getValue();
    if (LOGGER.isDebugEnabled() && FluentCompareTo.is(newPathCapacity)
      .greaterThan(originalVerifiedPathCapacitySnapshot)) {
      LOGGER.debug(
        "Increasing greatest path packet amount from {} to {}", originalVerifiedPathCapacitySnapshot, newPathCapacity
      );
    }
    this.verifiedPathCapacityRef.set(newPathCapacity);

    maxPacketAmountSnapshot.handle(
      maxPacketAmount -> { // <-- Handle UnknownMax
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("No path adjustment required. maxPacketAmount={}", maxPacketAmount);
        }
        return; // <-- Ignore: Can't adjust maxPacketAmount if we don't know a max yet.
      },
      maxPacketAmount -> { // <-- Handle ImpreciseMax
        if (newPathCapacity.equals(maxPacketAmountValue)) {
          // Binary search from F08s without metadata is complete: discovered precise max
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Discovered precise max packet amount: {}", maxPacketAmountSnapshot);
          }
          this.maxPacketAmountRef.set(MaxPacketAmount.builder()
            .maxPacketState(ImpreciseMax) // <-- Only F08 can set this to PreciseMax
            .value(maxPacketAmountValue) // <-- This is set properly above in the reduce method.
            .build());
        }
        return;
      },
      maxPacketAmount -> { // <-- Handle PreciseMax
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("No path adjustment required. maxPacketAmount={}", maxPacketAmount);
        }
        return;// <-- Ignore: Already know PreciseMax
      }
    );
  }

  public MaxPacketAmount getMaxPacketAmount() {
    return this.maxPacketAmountRef.get();
  }

  public boolean getNoCapacityAvailable() {
    return this.noCapacityAvailableRef.get();
  }

  public UnsignedLong verifiedPathCapacity() {
    return this.verifiedPathCapacityRef.get();
  }

  /**
   * Return a limit on the amount of the next packet: the precise max packet amount, or a probe amount if the precise
   * max packet amount is yet to be discovered.
   */
  public Optional<UnsignedLong> getNextMaxPacketAmount() {
    switch (this.maxPacketAmountRef.get().maxPacketState()) {
      case PreciseMax: {
        return this.maxPacketAmountRef.get().value(); // <-- When PreciseMax is set, the value must exist.
      }
      // Use a binary search to discover the precise max
      case ImpreciseMax: {
        final UnsignedLong verifiedPathCapacitySnapshot = this.verifiedPathCapacityRef.get();
        // Always positive: if verifiedCapacity=0, maxPacketAmount / 2 must round up to 1,
        // or if verifiedCapacity=maxPacketAmount, verifiedCapacity is positive, so adding it will always be positive
        return this.getMaxPacketAmount().value()
          .map(FluentUnsignedLong::of)
          .map(ful -> ful.minusOrZero(verifiedPathCapacitySnapshot))
          .map(ful -> ful.divideCeil(UnsignedLong.valueOf(2)))
          .map(FluentUnsignedLong::getValue)
          .map(ul -> ul.plus(verifiedPathCapacitySnapshot));
      }
      case UnknownMax:
      default: {
        // Do nothing.
        return Optional.empty();
      }
    }
  }

  /**
   * Return a known limit on the max-packet amount of the path, if it has been discovered. Requires that at least 1 unit
   * has been acknowledged by the receiver, and either the max packet amount has been precisely discovered, or no F08
   * has been encountered yet.
   */
  // TODO: Use this during rate-probe.
  public Optional<UnsignedLong> getDiscoveredMaxPacketAmount() {
    if (FluentUnsignedLong.of(verifiedPathCapacityRef.get()).isPositive()) {
      if (this.getMaxPacketAmount().maxPacketState() == PreciseMax) {
        return this.getMaxPacketAmount().value();
      } else if (this.getMaxPacketAmount().maxPacketState() == UnknownMax) {
        return Optional.of(UnsignedLong.MAX_VALUE);
      } else {
        return Optional.empty();
      }
    } else {
      return Optional.empty();
    }
  }

  public enum MaxPacketState {
    /**
     * Initial state before any F08 errors have been encountered.
     */
    UnknownMax,

    /**
     * F08 errors included metadata to communicate the precise max packet amount
     */
    PreciseMax,

    /**
     * F08 errors isolated an upper max packet amount, but didn't communicate it precisely. Discover the exact max
     * packet amount through probing.
     */
    ImpreciseMax,
  }

  /**
   * A Maximum packet amount for a particular payment path, with meta-data for how the amount was discovered.
   */
  @Immutable
  public interface MaxPacketAmount {

    static ImmutableMaxPacketAmount.Builder builder() {
      return ImmutableMaxPacketAmount.builder();
    }

    static MaxPacketAmount unknownMax() {
      return MaxPacketAmount.builder().maxPacketState(UnknownMax).build();
    }

    /**
     * Information around how {@link #value} was discovered.
     *
     * @return A {@link MaxPacketState}.
     */
    MaxPacketState maxPacketState();

    /**
     * The max packet amount for the ILP payment path. This value generally comes from an ILP F08 error.
     *
     * @return An {@link UnsignedLong}.
     */
    Optional<UnsignedLong> value();

    @Value.Check
    default void check() {
      if (maxPacketState() == ImpreciseMax || maxPacketState() == PreciseMax) {
        Preconditions.checkState(
          value().isPresent(), "MaxPacketAmount must have a value if the state is not `UnknownMax`"
        );
      }
    }

    /**
     * Execute various operations on this state object depending on the current state. If the current state is {@link
     * MaxPacketState#UnknownMax}, then {@code fulfillHandler} will be executed. Otherwise, if the current state is
     * {@link MaxPacketState#ImpreciseMax}, then {@code impreciseMaxHandler} will be executed. Otherwise, if the current
     * state is {@link MaxPacketState#PreciseMax}, then {@code preciseMaxHandler} will be executed.
     *
     * @param packetStateConsumer A {@link Function} to apply to this instance.
     */
    default <R> R map(final Function<MaxPacketAmount, R> packetStateConsumer) {
      Objects.requireNonNull(packetStateConsumer);
      return packetStateConsumer.apply(this);
    }

    /**
     * Execute various operations on this state object depending on the current state. If the current state is {@link
     * MaxPacketState#UnknownMax}, then {@code fulfillHandler} will be executed. Otherwise, if the current state is
     * {@link MaxPacketState#ImpreciseMax}, then {@code impreciseMaxHandler} will be executed. Otherwise, if the current
     * state is {@link MaxPacketState#PreciseMax}, then {@code preciseMaxHandler} will be executed.
     *
     * @param unknownMaxHandler   A {@link Consumer} to call if {@link #maxPacketState()} packet is {@link
     *                            MaxPacketState#UnknownMax}.
     * @param impreciseMaxHandler A {@link Consumer} to call if {@link #maxPacketState()} packet is {@link
     *                            MaxPacketState#ImpreciseMax}.
     * @param preciseMaxHandler   A {@link Consumer} to call if {@link #maxPacketState()} packet is {@link
     *                            MaxPacketState#PreciseMax}.
     */
    default void handle(
      final Consumer<MaxPacketAmount> unknownMaxHandler,
      final Consumer<MaxPacketAmount> impreciseMaxHandler,
      final Consumer<MaxPacketAmount> preciseMaxHandler
    ) {
      Objects.requireNonNull(unknownMaxHandler);
      Objects.requireNonNull(impreciseMaxHandler);
      Objects.requireNonNull(preciseMaxHandler);

      switch (this.maxPacketState()) {
        case UnknownMax: {
          unknownMaxHandler.accept(this);
          break;
        }
        case ImpreciseMax: {
          impreciseMaxHandler.accept(this);
          break;
        }
        case PreciseMax: {
          preciseMaxHandler.accept(this);
          break;
        }
        default: {
          throw new RuntimeException("Unhandled MaxPacketSate: " + this.maxPacketState());
        }
      }
    }

//    /**
//     * Execute various operations on this state object depending on the current state. If the current state is {@link
//     * MaxPacketState#UnknownMax}, then {@code fulfillHandler} will be executed. Otherwise, if the current state is
//     * {@link MaxPacketState#ImpreciseMax}, then {@code impreciseMaxHandler} will be executed. Otherwise, if the current
//     * state is {@link MaxPacketState#PreciseMax}, then {@code preciseMaxHandler} will be executed.
//     *
//     * @param unknownMaxHandler   A {@link Consumer} to call if {@link #maxPacketState()} packet is {@link
//     *                            MaxPacketState#UnknownMax}.
//     * @param impreciseMaxHandler A {@link Consumer} to call if {@link #maxPacketState()} packet is {@link
//     *                            MaxPacketState#ImpreciseMax}.
//     * @param preciseMaxHandler   A {@link Consumer} to call if {@link #maxPacketState()} packet is {@link
//     *                            MaxPacketState#PreciseMax}.
//     */
//    default MaxPacketAmount handleStateAndReturn(
//      final Consumer<MaxPacketAmount> unknownMaxHandler,
//      final Consumer<MaxPacketAmount> impreciseMaxHandler,
//      final Consumer<MaxPacketAmount> preciseMaxHandler
//    ) {
//      Objects.requireNonNull(unknownMaxHandler);
//      Objects.requireNonNull(impreciseMaxHandler);
//      Objects.requireNonNull(preciseMaxHandler);
//
//      switch (this.maxPacketState()) {
//        case UnknownMax: {
//          unknownMaxHandler.accept(this);
//          break;
//        }
//        case ImpreciseMax: {
//          impreciseMaxHandler.accept(this);
//          break;
//        }
//        case PreciseMax: {
//          preciseMaxHandler.accept(this);
//          break;
//        }
//        default: {
//          throw new RuntimeException("Unhandled MaxPacketSate: " + this.maxPacketState());
//        }
//      }
//      return this;
//    }
  }
}
