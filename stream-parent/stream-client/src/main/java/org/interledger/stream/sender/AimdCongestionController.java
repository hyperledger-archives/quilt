package org.interledger.stream.sender;

import static org.interledger.core.InterledgerErrorCode.T04_INSUFFICIENT_LIQUIDITY_CODE;

import org.interledger.codecs.stream.StreamCodecContextFactory;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.stream.AmountTooLargeErrorData;
import org.interledger.stream.StreamUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.UnsignedLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A basic congestion controller that implements an Additive Increase, Multiplicative Decrease (AIMD) congestion control
 * algorithm.
 *
 * @see "https://en.wikipedia.org/wiki/Additive_increase/multiplicative_decrease"
 */
public class AimdCongestionController implements CongestionController {

  private static final UnsignedLong TWO = UnsignedLong.valueOf(2L);
  private static final UnsignedLong HALF_UNSIGNED_MAX = UnsignedLong.MAX_VALUE.dividedBy(TWO);

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final UnsignedLong increaseAmount;
  private final BigDecimal decreaseFactor;
  private final CodecContext streamCodecContext;

  private AtomicReference<CongestionState> congestionState;
  private AtomicReference<UnsignedLong> amountInFlight;

  /**
   * The current maximum packet size that will be used.
   */
  private Optional<UnsignedLong> maxPacketAmount;

  /**
   * The maximum amount of value that can be "in flight" (i.e., waiting for a response) at any given moment in time.
   * This value fluctuates dynamically in response to stream conditions.
   */
  private AtomicReference<UnsignedLong> maxInFlight;

  public AimdCongestionController() {
    this(
        UnsignedLong.valueOf(1000L), UnsignedLong.valueOf(1000L), BigDecimal.valueOf(2.0),
        StreamCodecContextFactory.oer()
    );
  }

  /**
   * Required-args Constructor.
   *
   * @param startAmount        An {@link UnsignedLong} representing the packet amount to start a STREAM at.
   * @param increaseAmount     An {@link UnsignedLong} representing the amount to increase the {@link #maxInFlight} by
   *                           whenever a valid fulfillment is encountered. This value determines how quickly packet
   *                           sizes increase as the payment path continues to process larger and larger packets.
   * @param decreaseFactor     An {@link UnsignedLong} representing the amount to lower {@link #maxInFlight} by when the
   *                           receiver rejects a STREAM packet containing a T04 or non-F08 error code (F08 rejections
   *                           will contain information that can be used to reduce
   * @param streamCodecContext A {@link CodecContext} for encoding and decoding STREAM packets and frames.
   */
  public AimdCongestionController(
      final UnsignedLong startAmount, final UnsignedLong increaseAmount, final BigDecimal decreaseFactor,
      final CodecContext streamCodecContext
  ) {
    this.maxInFlight = new AtomicReference<>(Objects.requireNonNull(startAmount, "startAmount must not be null"));
    this.increaseAmount = Objects.requireNonNull(increaseAmount, "increaseAmount must not be null");
    this.decreaseFactor = Objects.requireNonNull(decreaseFactor, "decreaseFactor must not be null");
    this.streamCodecContext = Objects.requireNonNull(streamCodecContext, "streamCodecContext must not be null");

    this.congestionState = new AtomicReference<>(CongestionState.SLOW_START);
    this.amountInFlight = new AtomicReference<>(UnsignedLong.ZERO);

    this.maxPacketAmount = Optional.empty();
  }

  /**
   * <p>Compute the maximum packet amount that should be used if a new ILPv4 packet is going to be sent as part of this
   * Stream. This value is used by the stream client to form the next prepare packet size, and fluctuates based upon
   * prior stream activity.</p>
   *
   * <p>This computation depends on a sub-computation called `amountLeftInWindow`, which is the difference between the
   * {@link #maxInFlight} and the current {@link #amountInFlight}. The {@code maxAmount} returned by this function is
   * then computed by taking the min of `amountLeftInWindow` and {@link #maxPacketAmount}.</p>
   *
   * @return An {@link UnsignedLong} representing the current max packet amount for packets in this stream.
   */
  @Override
  public UnsignedLong getMaxAmount() {
    // A "window" is just an amount of time where some value is in-flight, and a StreamClient wants to put more
    // in-flight. Thus, the amount left in a window will trend towards zero as the amountInFlight increases.
    final UnsignedLong amountLeftInWindow = maxInFlight.get().minus(amountInFlight.get());

    // Synchronization is _probably not_ needed here since we're not assigning any values that could get corrupted, and
    // usage of this return value should be considered as a snapshot at a given moment of time anyway...
    return this.maxPacketAmount
        .map(maxPacketAmount -> StreamUtils.min(amountLeftInWindow, maxPacketAmount))
        .orElse(amountLeftInWindow);
  }

  @Override
  public void prepare(final UnsignedLong amount) {
    Objects.requireNonNull(amount);
    this.amountInFlight.getAndUpdate((currentAmountInFlight) -> currentAmountInFlight.plus(amount));
  }

  @Override
  public void fulfill(final UnsignedLong prepareAmount) {
    Objects.requireNonNull(prepareAmount);

    this.amountInFlight.getAndUpdate((currentAmountInFlight) -> currentAmountInFlight.minus(prepareAmount));

    // Before we know how much we should be sending at a time, double the window size on every successful packet.
    // Once we start getting errors, switch to Additive Increase, Multiplicative Decrease (AIMD) congestion avoidance.
    if (this.congestionState.get() == CongestionState.SLOW_START) {
      // Double the max in flight but don't exceed the u64 max value
      if (HALF_UNSIGNED_MAX.compareTo(this.maxInFlight.get()) >= 0) {
        this.maxInFlight.getAndUpdate(maxInFlight -> maxInFlight.times(TWO));
      } else {
        this.maxInFlight.set(UnsignedLong.MAX_VALUE);
      }
    } else {
      // Add to the max in flight but don't exceed the u64 max value
      if (UnsignedLong.MAX_VALUE.minus(increaseAmount).compareTo(this.maxInFlight.get()) >= 0) {
        this.maxInFlight.getAndUpdate(maxInFlight -> maxInFlight.plus(increaseAmount));
      } else {
        this.maxInFlight.set(UnsignedLong.MAX_VALUE);
      }
    }
  }

  @Override
  public void reject(final UnsignedLong prepareAmount, final InterledgerRejectPacket rejectPacket) {
    Objects.requireNonNull(prepareAmount);
    Objects.requireNonNull(rejectPacket);

    this.amountInFlight.getAndUpdate((currentAmountInFlight) -> currentAmountInFlight.minus(prepareAmount));

    switch (rejectPacket.getCode().getCode()) {

      /////////////////////////
      // Update the maxInFlight
      /////////////////////////
      case T04_INSUFFICIENT_LIQUIDITY_CODE: {
        congestionState.set(CongestionState.AVOID_CONGESTION);

        final UnsignedLong computedValue = UnsignedLong.valueOf(
            new BigDecimal(maxInFlight.get().bigIntegerValue()).divide(decreaseFactor, RoundingMode.FLOOR)
                .toBigInteger()
        );

        this.maxInFlight.set(StreamUtils.max(computedValue, UnsignedLong.ONE));

        logger.debug("For Congestion control purposes, handled T04 rejection. previousAmountInFlight={} "
                + "amountInFlight={} maxInFlight={}",
            amountInFlight.get().plus(prepareAmount), amountInFlight.get(), maxInFlight
        );

        break;
      }
      /////////////////////////
      // Update the maxPacketAmount
      /////////////////////////
      case InterledgerErrorCode.F08_AMOUNT_TOO_LARGE_CODE: {
        this.maxPacketAmount = Optional.of(this.handleF08Rejection(prepareAmount, rejectPacket));
        // Actual packet data is logged by the StreamSender, so no need to log packet details here.
        logger.debug("For Congestion control purposes, handled F08 rejection. previousAmountInFlight={} "
                + "amountInFlight={} maxInFlight={}",
            amountInFlight.get().plus(prepareAmount), amountInFlight.get(), maxInFlight
        );
        break;
      }
      default: {
        // No special treatment for unhandled errors, but warn just in case we start to see a lot of them.
        // Actual packet data is logged by the StreamSender, so no need to log packet details here.
        logger.warn("For Congestion control purposes, ignoring unhandled packet rejection ({}: {}).",
            rejectPacket.getCode().getCode(), rejectPacket.getCode().getName()
        );
      }
    }
  }

  /**
   * <p>Properly handle a rejection containing a {@link InterledgerErrorCode#F08_AMOUNT_TOO_LARGE} error code.</p>
   *
   * <p>The goal of this method is to reduce the {@link #maxPacketAmount} (if present) or set a maximum amount if no
   * value is present for {@link #maxPacketAmount}. This is because an F08 error fundamentally means the ILPv4 packet is
   * too big (see NOTE below). In order to do this, this function will first look into the data payload returned by the
   * Connector in {@link InterledgerRejectPacket#getData()}. If values can be computed from here, they will be.
   * Otherwise, the new `maxPacketAmount` will simply be half of {@code prepAmount}, or {@link UnsignedLong#ONE},
   * whichever is greater.</p>
   *
   * <p>
   * NOTE: This condition is not inherently a STREAM error, but is instead an ILPv4 error. From the ILPv4 RFC, an F08
   * error means: "The packet amount is higher than the maximum a connector is willing to forward. Senders MAY send
   * another packet with a lower amount. Connectors that produce this error SHOULD encode the amount they received and
   * their maximum in the data to help senders determine how much lower the packet amount should be." Thus, we can check
   * for an AmountTooLargeErrorData in the reject packet's data. If found, we use it. If not found, we just reduce by
   * the max packet amount by half of the prepared amount (because this value was too high, triggering the F08).
   * </p>
   *
   * @param prepareAmount An {@link UnsignedLong} representing the amount that was originally prepared to the immediate
   *                      peer inside of an {@link InterledgerPreparePacket}.
   * @param rejectPacket  The {@link InterledgerRejectPacket} that was returned from the immediate peer.
   *
   * @return An {@link UnsignedLong} representing the new value of {@link #maxPacketAmount}.
   */
  @VisibleForTesting
  protected UnsignedLong handleF08Rejection(
      final UnsignedLong prepareAmount, final InterledgerRejectPacket rejectPacket
  ) {
    Objects.requireNonNull(prepareAmount, "prepareAmount must not be null");
    Objects.requireNonNull(rejectPacket, "rejectPacket must not be null");

    // Compute the newMaxPacketAmount
    UnsignedLong newMaxPacketAmount;
    if (rejectPacket.getData().length > 0) {
      try {
        // Assume there's error data, because their should be.
        final AmountTooLargeErrorData amountTooLargeErrorData =
            streamCodecContext.read(AmountTooLargeErrorData.class, new ByteArrayInputStream(rejectPacket.getData()));

        final BigDecimal prepareAmountAsBigDecimal = new BigDecimal(prepareAmount.bigIntegerValue());
        final BigDecimal detailsMaxAmount = new BigDecimal(amountTooLargeErrorData.maximumAmount().bigIntegerValue());
        final BigDecimal detailsAmountReceived = new BigDecimal(
            amountTooLargeErrorData.receivedAmount().bigIntegerValue());

        if (detailsAmountReceived.equals(BigDecimal.ZERO)) {
          newMaxPacketAmount = halvePrepareAmount(prepareAmount);
        } else {
          // Prepared 10, but only sent 3, max is 2
          // receivedAmount: Local amount received by the connector
          // maxAmount: Maximum amount (inclusive, denominated in same units as the receivedAmount) the connector
          // will forward
          // Equation: new_max_packet_amount = prepare_amount * details.max_amount() / details.amount_received();
          final BigDecimal newMaxPacketAmountAsBigDecimal =
              prepareAmountAsBigDecimal.multiply(detailsMaxAmount).divide(detailsAmountReceived, RoundingMode.FLOOR);
          newMaxPacketAmount = UnsignedLong.valueOf(newMaxPacketAmountAsBigDecimal.toBigIntegerExact());
        }
      } catch (Exception e) {
        // log a warning, but otherwise eat this exception. We'll continue on using default reduction values.
        logger.warn("Unable to decode AmountTooLargeErrorData from F08 Reject packet. Setting newMaxPacketAmount to be "
            + "half the prepare amount. rejectPacket={} error={}", rejectPacket, e
        );
        newMaxPacketAmount = halvePrepareAmount(prepareAmount);
      }
    } else {
      newMaxPacketAmount = halvePrepareAmount(prepareAmount);
      logger.warn(
          "F08 Reject packet had no data payload.  Setting newMaxPacketAmount to be {} (half the prepare amount)",
          newMaxPacketAmount
      );
    }

    final UnsignedLong newMaxPacketAmountFinal = newMaxPacketAmount;
    return this.maxPacketAmount
        // If maxPacketAmount is present, take the lower of it or newMaxPacketAmount
        .map($ -> StreamUtils.min($, newMaxPacketAmountFinal))
        // Otherwise, just use newMaxPacketAmount
        .orElse(newMaxPacketAmount);
  }

  @VisibleForTesting
  protected UnsignedLong halvePrepareAmount(final UnsignedLong prepareAmount) {
    Objects.requireNonNull(prepareAmount);
    return StreamUtils.max(prepareAmount.dividedBy(TWO), UnsignedLong.ONE); // division rounds down.
  }

  /**
   * Accessor for the current congestion state.
   *
   * @return A {@link CongestionState} representing the current state.
   */
  @Override
  public CongestionState getCongestionState() {
    return this.congestionState.get();
  }

  /**
   * Sets the current congestion state.
   *
   * @param congestionState A new {@link CongestionState}.
   */
  public void setCongestionState(final CongestionState congestionState) {
    Objects.requireNonNull(congestionState);
    this.congestionState.set(congestionState);
  }

  @Override
  public Optional<UnsignedLong> getMaxPacketAmount() {
    return maxPacketAmount;
  }

  public void setMaxPacketAmount(final UnsignedLong maxPacketAmount) {
    this.maxPacketAmount = Optional.of(maxPacketAmount);
  }

  public void setMaxPacketAmount(final Optional<UnsignedLong> maxPacketAmount) {
    this.maxPacketAmount = Objects.requireNonNull(maxPacketAmount);
  }

  @Override
  public boolean hasInFlight() {
    return this.amountInFlight.get().compareTo(UnsignedLong.ZERO) > 0;
  }

  /**
   * The current state of congestion.
   */
  enum CongestionState {
    SLOW_START,
    AVOID_CONGESTION
  }
}
