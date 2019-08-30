package org.interledger.stream.congestion;

import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerRejectPacket;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.UnsignedLong;

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

  private final UnsignedLong increaseAmount;
  private final BigDecimal decreaseFactor;

  private AtomicReference<CongestionState> congestionState;
  private AtomicReference<UnsignedLong> amountInFlight;

  private Optional<UnsignedLong> maxPacketAmount;

  // unsigned!
  private UnsignedLong maxInFlight;

  public AimdCongestionController() {
    this(UnsignedLong.valueOf(1000L), UnsignedLong.valueOf(1000L), BigDecimal.valueOf(2.0));
  }

  /**
   * Required-args Constructor.
   *
   * @param startAmount
   * @param increaseAmount
   * @param decreaseFactor
   */
  public AimdCongestionController(
      final UnsignedLong startAmount, final UnsignedLong increaseAmount, final BigDecimal decreaseFactor
  ) {
    this.maxInFlight = Objects.requireNonNull(startAmount, "startAmount must not be null");
    this.increaseAmount = Objects.requireNonNull(increaseAmount, "increaseAmount must not be null");
    this.decreaseFactor = Objects.requireNonNull(decreaseFactor, "decreaseFactor must not be null");

    this.congestionState = new AtomicReference<>(CongestionState.SLOW_START);
    this.amountInFlight = new AtomicReference<>(UnsignedLong.ZERO);

    this.maxPacketAmount = Optional.empty();
  }

  /**
   * <p>Compute the maximum packet amount for the controller.</p>
   *
   * <p>This computation depends on a sub-computation called `amountLeftInWindow`, which is the difference between the
   * {@link #maxInFlight} and the current {@link #amountInFlight}. The maxAmount is then computed by taking the min of
   * `amountLeftInWindow` and {@link #maxPacketAmount}.</p>
   *
   * @return The current max amount for this stream.
   */
  // TODO: Determine if this data needs to be synchronized.
  public UnsignedLong getMaxAmount() {
    final UnsignedLong amountLeftInWindow = maxInFlight.minus(amountInFlight.get());
    return this.maxPacketAmount
        // If maxInFlight is specified, take the min of `amountLeftInWindow` and `maxInFlight`.
        .map(maxPacketAmount -> min(amountLeftInWindow, maxPacketAmount))
        .orElse(amountLeftInWindow);
  }

  @Override
  public void prepare(final UnsignedLong amount) {
    Objects.requireNonNull(amount);
    this.amountInFlight.getAndUpdate((currentAmountInFlight) -> currentAmountInFlight.plus(amount));
  }

  public void fulfill(final UnsignedLong prepareAmount) {
    Objects.requireNonNull(prepareAmount);

    this.amountInFlight.getAndUpdate((currentAmountInFlight) -> currentAmountInFlight.minus(prepareAmount));

    // Before we know how much we should be sending at a time, double the window size on every successful packet.
    // Once we start getting errors, switch to Additive Increase, Multiplicative Decrease (AIMD) congestion avoidance.
    if (this.congestionState.get() == CongestionState.SLOW_START) {
      // Double the max in flight but don't exceed the u64 max value
      if (UnsignedLong.MAX_VALUE.dividedBy(TWO).compareTo(this.maxInFlight) >= 0) {
        this.maxInFlight = maxInFlight.times(TWO);
      } else {
        this.maxInFlight = UnsignedLong.MAX_VALUE;
      }
    } else {
      // Add to the max in flight but don't exceed the u64 max value
      if (UnsignedLong.MAX_VALUE.minus(increaseAmount).compareTo(this.maxInFlight) >= 0) {
        this.maxInFlight = maxInFlight.plus(increaseAmount);
      } else {
        this.maxInFlight = UnsignedLong.MAX_VALUE;
      }
    }
  }

  public void reject(final UnsignedLong prepareAmount, final InterledgerRejectPacket rejectPacket) {
    Objects.requireNonNull(prepareAmount);
    Objects.requireNonNull(rejectPacket);

    this.amountInFlight.getAndUpdate((currentAmountInFlight) -> currentAmountInFlight.minus(prepareAmount));

    switch (rejectPacket.getCode().getCode()) {
      case InterledgerErrorCode.T04_INSUFFICIENT_LIQUIDITY_CODE: {
        congestionState.set(CongestionState.AVOID_CONGESTION);

        final UnsignedLong computedValue = UnsignedLong.valueOf(
            new BigDecimal(maxInFlight.bigIntegerValue()).divide(decreaseFactor, RoundingMode.FLOOR).toBigInteger());

        if (computedValue.compareTo(UnsignedLong.ONE) > 0) {
          maxInFlight = computedValue;
        } else {
          maxInFlight = UnsignedLong.ONE;
        }

        // Rejected packet with T04 error. amountInFlight={}, decreasing max in flight to maxInFlight={}",
        // amountInFlight.plus(prepare_amount), maxInFlight);
        return;
      }
      case InterledgerErrorCode.F08_AMOUNT_TOO_LARGE_CODE: {

        if (rejectPacket.getData().length > 0) {
          BigDecimal prepareAmountBig = new BigDecimal(prepareAmount.bigIntegerValue());

          //TODO: Get these values from the MaxPacketAmountDetails
          final BigDecimal detailsMaxAmount = BigDecimal.valueOf(1L);
          final BigDecimal detailsAmountReceived = BigDecimal.valueOf(1L);

          final BigDecimal newMaxPacketAmountAsBigDecimal = (prepareAmountBig.multiply(detailsMaxAmount))
              .divide(detailsAmountReceived);
          final UnsignedLong newMaxPacketAmount = UnsignedLong
              .valueOf(newMaxPacketAmountAsBigDecimal.toBigIntegerExact());

          this.maxPacketAmount = Optional.ofNullable(this.maxPacketAmount
              // If maxInFlight is specified, take the min of `maxPacketAmount` and `newMaxPacketAmount`.
              .map(maxPacketAmount -> min(maxPacketAmount, newMaxPacketAmount))
              // Otherwise, just set the maxPacketAmount to be newMaxPacketAmount
              .orElse(newMaxPacketAmount)
          );
        } else {
          // TODO lower the max packet amount anyway.
          // See https://github.com/interledger-rs/interledger-rs/issues/266
        }
      }
      default: {
        // No special treatment for unhandled errors.
      }
    }
  }

  @VisibleForTesting
  protected UnsignedLong min(final UnsignedLong v1, final UnsignedLong v2) {
    Objects.requireNonNull(v1);
    Objects.requireNonNull(v2);

    if (v1.compareTo(v2) < 0) {
      return v1;
    } else {
      return v2;
    }
  }

  /**
   * Accessor for the current congestion state.
   *
   * @return A {@link CongestionState} representing the current state.
   */
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

  public Optional<UnsignedLong> getMaxPacketAmount() {
    return maxPacketAmount;
  }

  public void setMaxPacketAmount(final UnsignedLong maxPacketAmount) {
    this.maxPacketAmount = Optional.of(maxPacketAmount);
  }

  public void setMaxPacketAmount(final Optional<UnsignedLong> maxPacketAmount) {
    this.maxPacketAmount = Objects.requireNonNull(maxPacketAmount);
  }

  /**
   * The current state of congestion.
   */
  enum CongestionState {
    SLOW_START,
    AVOID_CONGESTION
  }
}
