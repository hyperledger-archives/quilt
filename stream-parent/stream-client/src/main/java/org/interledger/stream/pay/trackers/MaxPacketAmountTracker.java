package org.interledger.stream.pay.trackers;

import com.google.common.primitives.UnsignedLong;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
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

  private final static Logger logger = LoggerFactory.getLogger(MaxPacketAmountTracker.class);

  /**
   * How to interpret the {@code maxPacketAmount}.
   */
  private final AtomicReference<MaxPacketAmount> maxPacketAmount = new AtomicReference<>(MaxPacketAmount.unknownMax());

  /**
   * Greatest amount the recipient acknowledged to have received. Note: this is always reduced so it's never greater
   * than the max packet amount
   */
  private final AtomicReference<UnsignedLong> verifiedPathCapacity = new AtomicReference<>(UnsignedLong.ZERO);

  /**
   * Is the max packet amount 0 and impossible to send value over this path?
   */
  private final AtomicBoolean noCapacityAvailable = new AtomicBoolean();

  /**
   * Decrease the path max packet amount in response to an F08 ILP rejection.
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
      .map(amountTooLargeErrorData -> {

        final UnsignedLong totalReceivedByRemote = amountTooLargeErrorData.receivedAmount();
        final UnsignedLong remoteMaximum = amountTooLargeErrorData.maximumAmount();

        if (logger.isDebugEnabled()) {
          logger.debug("Handling F08 Reject packet. amountTooLargeErrorData={}", amountTooLargeErrorData);
        }

        // F08 is invalid if the remote received less than their own maximum! This check ensures that remoteReceived
        // is always > 0
        if (FluentCompareTo.is(totalReceivedByRemote).greaterThan(remoteMaximum)) {
          return Optional.<MaxPacketAmount>empty();
        }

        // Convert remote max packet amount into source units
        final Ratio exchangeRate = Ratio.from(sourceAmount, totalReceivedByRemote);
        final UnsignedLong newMaxAmount = FluentUnsignedLong.of(remoteMaximum).timesFloor(exchangeRate).getValue();

        return Optional.<MaxPacketAmount>of(
          MaxPacketAmount.builder().maxPacketState(MaxPacketState.PreciseMax).value(newMaxAmount).build()
        );
      })
      // There was no amountTooLargeErrorData that could be found, so the only thing we can infer is that the amount
      // sent was too high. So, simply reduce by ONE.
      .orElseGet(() -> Optional.of(MaxPacketAmount.builder().maxPacketState(MaxPacketState.ImpreciseMax)
        .value(sourceAmount.minus(UnsignedLong.ONE)).build()))
      .filter(newMaxPacketAmount -> newMaxPacketAmount.value().isPresent())
      .ifPresent(newMaxPacketAmount -> reduceMaxPacketAmount(
        // Per the filter above, newMaxPacketAmountValue is present.
        this.maxPacketAmount.get().maxPacketState(),
        newMaxPacketAmount.maxPacketState(),
        newMaxPacketAmount.value().get(),
        sourceAmount
        )
      );
  }

  /**
   * Decrease the path max packet amount in response to F08 errors.
   */
  private void reduceMaxPacketAmount(
    final MaxPacketState currenMaxPacketStateSnapshot,
    final MaxPacketState newMaxPacketState,
    final UnsignedLong newMaxPacketAmountValue,
    final UnsignedLong sourceAmount
  ) {
    Objects.requireNonNull(newMaxPacketAmountValue);
    Objects.requireNonNull(newMaxPacketState);
    Objects.requireNonNull(sourceAmount);

    // Special case if max packet is 0 or rounds to 0
    if (FluentCompareTo.is(newMaxPacketAmountValue).lessThanOrEqualTo(UnsignedLong.ZERO)) {
      if (logger.isDebugEnabled()) {
        logger.debug("Ending payment: max packet amount is 0, cannot send over path");
      }
      this.noCapacityAvailable.set(true);
      return;
    }

    if (currenMaxPacketStateSnapshot == MaxPacketState.UnknownMax) {
      final MaxPacketAmount newMaxPacket = MaxPacketAmount.builder()
        .maxPacketState(newMaxPacketState)
        .value(newMaxPacketAmountValue)
        .build();
      if (logger.isDebugEnabled()) {
        logger.debug("Setting initial maxPacketAmount to {}", newMaxPacket);
      }
      this.maxPacketAmount.set(newMaxPacket);
    } else {
      // State is known.

      // Use zero because newMax will never be less than this amount, and we only want the if to reduce if that's
      // the case.
      final MaxPacketAmount currentMaxPacketAmount = this.maxPacketAmount.get();
      final UnsignedLong currentMaxPacketAmountValue = currentMaxPacketAmount.value().orElse(UnsignedLong.ZERO);
      if (FluentCompareTo.is(newMaxPacketAmountValue).lessThan(currentMaxPacketAmountValue)) {
        final MaxPacketAmount newMaxPacketAmount = MaxPacketAmount.builder()
          .maxPacketState(newMaxPacketState)
          .value(newMaxPacketAmountValue)
          .build();
        if (logger.isDebugEnabled()) {
          logger.debug("Reducing max packet amount from {} to {}", currentMaxPacketAmount, newMaxPacketAmount);
          this.maxPacketAmount.set(newMaxPacketAmount);
        } else {
          // Ignore F08s that don't lower the max packet amount
          return;
        }
      }

      // If we get this far, adjust the path capacity with the current verified capacity because this will become
      // verified if we have a precise maxPacketAmount.
      this.adjustPathCapacity(this.verifiedPathCapacity.get());
    }
  }

  /**
   * Increase the greatest amount acknowledged by the recipient, which indicates the path is capable of sending packets
   * of at least that amount.
   */
  // TODO: If this is called from nextState, then no need to synchronize.
  public synchronized void adjustPathCapacity(final UnsignedLong ackAmount) {
    final MaxPacketAmount maxPacketAmountSnapshot = this.maxPacketAmount.get();

    final UnsignedLong maxPacketAmountValueSnapshot;
    if (maxPacketAmountSnapshot.maxPacketState() != MaxPacketState.UnknownMax && maxPacketAmountSnapshot.value()
      .isPresent()) {
      maxPacketAmountValueSnapshot = maxPacketAmountSnapshot.value().get();
    } else {
      maxPacketAmountValueSnapshot = UnsignedLong.MAX_VALUE;
    }

    final UnsignedLong verifiedPathCapacitySnapshot = this.verifiedPathCapacity.get();
    final UnsignedLong newPathCapacity = FluentUnsignedLong.of(this.verifiedPathCapacity.get()).orGreater(ackAmount)
      .orLesser(maxPacketAmountValueSnapshot).getValue();
    if (logger.isDebugEnabled() && FluentCompareTo.is(newPathCapacity).greaterThan(verifiedPathCapacitySnapshot)) {
      logger.debug(
        "Increasing greatest path packet amount from {} to {}", verifiedPathCapacitySnapshot, newPathCapacity
      );
    }
    this.verifiedPathCapacity.set(newPathCapacity);

    // State is only set to ImpreciseMax when we reduce by one above because we can't interpret the F08 data payload.
    if (maxPacketAmountSnapshot.maxPacketState() == MaxPacketState.ImpreciseMax &&
      newPathCapacity.equals(maxPacketAmountValueSnapshot)
    ) {
      // Binary search from F08s without metadata is complete: discovered precise max
      logger.debug("Discovered precise max packet amount: {}", maxPacketAmountSnapshot);
      this.maxPacketAmount
        .set(MaxPacketAmount.builder()
          .maxPacketState(MaxPacketState.PreciseMax)
          .value(maxPacketAmountValueSnapshot)
          .build()
        );
    }

  }

  public MaxPacketAmount getMaxPacketAmount() {
    return maxPacketAmount.get();
  }

  public boolean isNoCapacityAvailable() {
    return noCapacityAvailable.get();
  }

  /**
   * Return a limit on the amount of the next packet: the precise max packet amount, or a probe amount if the precise
   * max packet amount is yet to be discovered.
   */
  public Optional<UnsignedLong> getNextMaxPacketAmount() {
    switch (this.maxPacketAmount.get().maxPacketState()) {
      case PreciseMax: {
        return this.maxPacketAmount.get().value(); // <-- When PreciseMax is set, the value must exist.
      }
      // Use a binary search to discover the precise max
      case ImpreciseMax: {
        // Always positive: if verifiedCapacity=0, maxPacketAmount / 2
        // must round up to 1, or if verifiedCapacity=maxPacketAmount,
        // verifiedCapacity is positive, so adding it will always be positive
        return this.maxPacketAmount.get().value()
          .map(FluentUnsignedLong::of)
          .map(ful -> ful.minusOrZero(this.verifiedPathCapacity.get()))
          .map(ful -> ful.divideCeil(UnsignedLong.valueOf(2)))
          .map(FluentUnsignedLong::getValue)
          .map(ul -> ul.plus(this.verifiedPathCapacity.get()));
      }
      case UnknownMax:
      default: {
        // Do nothing.
        return Optional.empty();
      }
    }
  }

  public enum MaxPacketState {
    /**
     * Initial state before any F08 errors have been encountered
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
   * A Max packet amount with meta-data for how the amount was discovered.
   */
  @Immutable
  public interface MaxPacketAmount {

    static ImmutableMaxPacketAmount.Builder builder() {
      return ImmutableMaxPacketAmount.builder();
    }

    static MaxPacketAmount unknownMax() {
      return MaxPacketAmount.builder().maxPacketState(MaxPacketState.UnknownMax).build();
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

  }

}
