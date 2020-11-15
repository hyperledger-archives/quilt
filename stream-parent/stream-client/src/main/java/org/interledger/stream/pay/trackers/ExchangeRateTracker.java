package org.interledger.stream.pay.trackers;

import com.google.common.collect.Maps;
import com.google.common.primitives.UnsignedLong;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.interledger.core.fluent.FluentCompareTo;
import org.interledger.core.fluent.Ratio;
import org.interledger.stream.pay.exceptions.StreamPayerException;
import org.interledger.stream.pay.model.ExchangeRateBound;
import org.interledger.stream.pay.model.SendState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks the current exchange-rate for a given STREAM payment.
 */
public class ExchangeRateTracker {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  /**
   * Realized exchange rate is greater than or equal to this ratio (inclusive) (i.e., destination / source).
   */
  private final AtomicReference<Ratio> lowerBoundRate;

  /**
   * The realized exchange-rate is less than this ratio (exclusive) (i.e., destination / source).
   *
   * @returnExchangeRateTracker
   */
  private final AtomicReference<Ratio> upperBoundRate;

  public ExchangeRateTracker() {
    // These start off as null so that the greater/less-than code doesn't skip the first values.
    this.lowerBoundRate = new AtomicReference<>();
    this.upperBoundRate = new AtomicReference<>();
  }

  /**
   * Mapping of packet received amounts to its most recent sent amount.
   */
  private Map<UnsignedLong, UnsignedLong> sentAmounts = Maps.newConcurrentMap();

  /**
   * Mapping of packet sent amounts to its most recent received amount.
   */
  private Map<UnsignedLong, UnsignedLong> receivedAmounts = Maps.newConcurrentMap();

  /**
   * Update the current rate for this payment based upon the supplied values. Because intermediaries floor packet
   * amounts, the exchange rate cannot be precisely computed: it's only known with some margin. However, as we send
   * packets of varying sizes, the upper and lower bounds should converge closer and closer to the real exchange rate.
   *
   * @param sourceAmount   An {@link UnsignedLong} representing the source amount (i.e., the amount sent).
   * @param receivedAmount An {@link UnsignedLong} representing the received amount.
   */
  public synchronized void updateRate(final UnsignedLong sourceAmount, final UnsignedLong receivedAmount) {
    Objects.requireNonNull(sourceAmount);
    Objects.requireNonNull(receivedAmount);

    final Ratio packetLowerBoundRate = Ratio.from(sourceAmount, receivedAmount);
    final Ratio packetUpperBoundRate = Ratio.from(sourceAmount.plus(UnsignedLong.ONE), receivedAmount);

    // If the exchange rate fluctuated and is "out of bounds," reset it
    Optional.ofNullable(this.receivedAmounts.get(sourceAmount)).ifPresent(previousReceivedAmount ->
    {
      final boolean shouldResetExchangeRate = (FluentCompareTo.is(previousReceivedAmount).equalTo(receivedAmount) ||
        FluentCompareTo.is(packetUpperBoundRate).lessThanOrEqualTo(this.lowerBoundRate.get()) ||
        FluentCompareTo.is(packetLowerBoundRate).greaterThanEqualTo(this.upperBoundRate.get()));
      if (shouldResetExchangeRate) {
        logger.debug(
          "Exchange rate changed. resetting to [{}, {}]",
          packetLowerBoundRate, packetUpperBoundRate
        );
        this.upperBoundRate.set(packetUpperBoundRate);
        this.lowerBoundRate.set(packetLowerBoundRate);
        this.sentAmounts.clear();
        this.receivedAmounts.clear();
      }
    });

    if (this.lowerBoundRate.get() == null || FluentCompareTo.is(packetLowerBoundRate)
      .greaterThan(this.lowerBoundRate.get())) {
      logger.debug("Increasing probed rate lower bound from {} to {}", this.lowerBoundRate.get(), packetLowerBoundRate);
      this.lowerBoundRate.set(packetLowerBoundRate);
    }

    if (this.upperBoundRate.get() == null || FluentCompareTo.is(packetUpperBoundRate)
      .lessThan(this.upperBoundRate.get())) {
      logger.debug("Reducing probed rate upper bound from {} to {}", this.upperBoundRate.get(), packetUpperBoundRate);
      this.upperBoundRate.set(packetUpperBoundRate);
    }

    this.sentAmounts.put(receivedAmount, sourceAmount);
    this.receivedAmounts.put(sourceAmount, receivedAmount);

  }

  /**
   * Estimate the delivered amount from the given source amount.
   *
   * <ol>
   *   <li>Low-end estimate: at least this amount will get delivered, if the rate hasn't fluctuated.</li>
   *   <li>High-end estimate: no more than this amount will get delivered, if the rate hasn't fluctuated.</li>
   * </ol>
   */
  public synchronized ExchangeRateBound estimateDestinationAmount(final UnsignedLong sourceAmount) {
    // If we already sent a packet for this amount, return how much the recipient got
    return Optional.ofNullable(this.receivedAmounts.get(sourceAmount))
      .map(amountReceived -> ExchangeRateBound.builder().lowEndEstimate(amountReceived).highEndEstimate(amountReceived)
        .build())
      .orElseGet(() -> {

        // TODO: What happens if the lowerBound and upperBounds aren't set yet? It's likely that if receivedAmounts
        // is not empty, then these will be populated, but worth a unit test.
        UnsignedLong lowEndDestination = getLowerBoundRate().multiplyFloor(sourceAmount);

        // Because the upperBound exchange rate is exclusive:
        // If source amount converts exactly to an integer, destination amount MUST be 1 unit less
        // If source amount doesn't convert precisely, we can't narrow it any better than that amount, floored ¯\_(ツ)_/¯
        UnsignedLong highEndDestination = getUpperBoundRate().multiplyCeil(sourceAmount).minus(UnsignedLong.ONE);
        return ExchangeRateBound.builder().lowEndEstimate(lowEndDestination).highEndEstimate(highEndDestination)
          .build();
      });
  }

  public Ratio getLowerBoundRate() {
    if (lowerBoundRate.get() == null) {
      throw new StreamPayerException("No lowerBoundRate was detected from the receiver.", SendState.RateProbeFailed);
    }
    return lowerBoundRate.get();
  }

  public Ratio getUpperBoundRate() {
    if (upperBoundRate.get() == null) {
      throw new StreamPayerException("No upperBoundRate was detected from the receiver.", SendState.RateProbeFailed);
    }
    return upperBoundRate.get();
  }

}
