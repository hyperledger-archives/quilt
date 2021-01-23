package org.interledger.stream.pay.trackers;

import org.interledger.core.fluent.FluentBigInteger;
import org.interledger.core.fluent.FluentCompareTo;
import org.interledger.core.fluent.FluentUnsignedLong;
import org.interledger.core.fluent.Ratio;
import org.interledger.fx.Denomination;
import org.interledger.fx.OracleExchangeRateService;
import org.interledger.fx.Slippage;
import org.interledger.stream.pay.exceptions.StreamPayerException;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.probing.model.DeliveredExchangeRateBound;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.primitives.UnsignedLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tracks the current exchange-rate for a given STREAM payment.
 */
public class ExchangeRateTracker {

  private static final Logger logger = LoggerFactory.getLogger(ExchangeRateTracker.class);

  /**
   * Realized exchange rate is greater than or equal to this ratio (inclusive) (i.e., destination / source).
   */
  private final AtomicReference<Ratio> lowerBoundRate;

  /**
   * The realized exchange-rate is less than this ratio (exclusive) (i.e., destination / source).
   */
  private final AtomicReference<Ratio> upperBoundRate;

  private final OracleExchangeRateService oracleExchangeRateService;

  /**
   * No-args Constructor.
   *
   * @param oracleExchangeRateService An {@link OracleExchangeRateService}.
   */
  public ExchangeRateTracker(final OracleExchangeRateService oracleExchangeRateService) {
    this.oracleExchangeRateService = Objects.requireNonNull(oracleExchangeRateService);

    // These start off as null so that the greater/less-than code doesn't skip the first values.
    this.lowerBoundRate = new AtomicReference<>();
    this.upperBoundRate = new AtomicReference<>();
  }

  /**
   * Mapping of packet received amounts to its most recent sent amount. These values are typed as {@link UnsignedLong}
   * because ILP Prepare packets may never exceed `MaxUInt64`, which is equal to {@link UnsignedLong#MAX_VALUE}.
   */
  private final Map<UnsignedLong, UnsignedLong> sentAmounts = Maps.newConcurrentMap();

  /**
   * Mapping of packet sent amounts to its most recent received amount. These values are typed as {@link UnsignedLong} *
   * because ILP Prepare packets may never exceed `MaxUInt64`, which is equal to {@link UnsignedLong#MAX_VALUE}.
   */
  private final Map<UnsignedLong, UnsignedLong> receivedAmounts = Maps.newConcurrentMap();

  /**
   * Initialize this tracker to indicate an exchange rate as supplied from an external FX oracle.
   *
   * @param sourceDenomination      A {@link Denomination} for the source account.
   * @param destinationDenomination A {@link Denomination} for the destination account.
   */
  public synchronized void initializeRates(
    final Denomination sourceDenomination, final Denomination destinationDenomination

  ) {
    Objects.requireNonNull(sourceDenomination);
    Objects.requireNonNull(destinationDenomination);

    final BigDecimal scaledExternalExchangeRate = this.oracleExchangeRateService.getScaledExchangeRate(
      sourceDenomination, destinationDenomination, Slippage.NONE
    );

    this.sentAmounts.clear();
    this.receivedAmounts.clear();

    final Ratio lowerBoundRate = Ratio.from(scaledExternalExchangeRate);
    final Ratio upperBoundRate = Ratio.builder()
      .from(lowerBoundRate)
      .numerator(lowerBoundRate.numerator().add(BigInteger.ONE))
      .build();

    logger.debug("Initializing exchange rate to [{}, {}]", lowerBoundRate, upperBoundRate);

    this.upperBoundRate.set(upperBoundRate);
    this.lowerBoundRate.set(lowerBoundRate);

    final UnsignedLong sourceAmount = FluentBigInteger.of(lowerBoundRate.denominator()).orMaxUnsignedLong();
    final UnsignedLong destinationAmount = FluentBigInteger.of(lowerBoundRate.numerator()).orMaxUnsignedLong();
    this.sentAmounts.put(destinationAmount, sourceAmount);
    this.receivedAmounts.put(sourceAmount, destinationAmount);
  }

  /**
   * Update the current rate for this payment based upon the supplied values. Because intermediaries floor packet
   * amounts, the exchange rate cannot be precisely computed: it's only known within some margin. However, as we send
   * packets of varying sizes, the upper and lower bounds should converge closer and closer to the real exchange rate.
   *
   * @param sourceAmount   An {@link UnsignedLong} representing the source amount (i.e., the amount sent).
   * @param receivedAmount An {@link UnsignedLong} representing the received amount.
   */
  public synchronized void updateRate(final UnsignedLong sourceAmount, final UnsignedLong receivedAmount) {
    Objects.requireNonNull(sourceAmount);
    Objects.requireNonNull(receivedAmount);

    final Ratio packetUpperBoundRate = Ratio.from(receivedAmount.plus(UnsignedLong.ONE), sourceAmount);
    final Ratio packetLowerBoundRate = Ratio.from(receivedAmount, sourceAmount);

    // If the exchange rate fluctuated and is "out of bounds," reset it
    Optional.ofNullable(this.receivedAmounts.get(sourceAmount)).ifPresent(previousReceivedAmount -> {
      if (shouldResetExchangeRate(packetLowerBoundRate, packetUpperBoundRate, previousReceivedAmount, receivedAmount)) {
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

    final Ratio lowerBoundRateSnapshot = this.lowerBoundRate.get();
    final Ratio upperBoundRateSnapshot = this.upperBoundRate.get();

    if (lowerBoundRateSnapshot == null ||
      FluentCompareTo.is(packetLowerBoundRate).greaterThan(lowerBoundRateSnapshot) ||
      packetLowerBoundRate.equalButMorePrecise(lowerBoundRateSnapshot) // <-- More precise wins when equal!
    ) {
      logger.debug("Increasing probed rate lower bound from {} to {}", lowerBoundRateSnapshot, packetLowerBoundRate);
      this.lowerBoundRate.set(packetLowerBoundRate);
    }

    if (upperBoundRateSnapshot == null ||
      FluentCompareTo.is(packetUpperBoundRate).lessThan(upperBoundRateSnapshot) ||
      packetUpperBoundRate.equalButMorePrecise(upperBoundRateSnapshot) // <-- More precise wins when equal!
    ) {
      logger.debug("Reducing probed rate upper bound from {} to {}", upperBoundRateSnapshot, packetUpperBoundRate);
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
   *
   * @param sourceAmount An {@link UnsignedLong} representing the source amount of a packet send.
   *
   * @return A {@link DeliveredExchangeRateBound}.
   */
  public synchronized DeliveredExchangeRateBound estimateDestinationAmount(final UnsignedLong sourceAmount) {
    Objects.requireNonNull(sourceAmount);

    // If we already sent a packet for this amount, return how much the recipient got
    return Optional.ofNullable(this.receivedAmounts.get(sourceAmount))
      .map(amountReceived ->
        DeliveredExchangeRateBound.builder().lowEndEstimate(amountReceived).highEndEstimate(amountReceived).build()
      )
      .orElseGet(() -> {
        // Because ILPv4 only accepts UInt64, anything larger than MaxUInt64 will deliver no value, likely because the
        // receiver will overflow and reject the packet. Thus, if we compute that the calculating lowEndDestination
        // will overflow, we merely set it to 0 so that no send get attempted. Same for highEndDestination.
        final UnsignedLong lowEndDestination = getLowerBoundRate().multiplyFloorOrZero(sourceAmount);

        // Because the upperBound exchange rate is exclusive:
        // If source amount converts exactly to an integer, destination amount MUST be 1 unit less
        // If source amount doesn't convert precisely, we can't narrow it any better than that amount, floored ¯\_(ツ)_/¯
        final UnsignedLong highEndDestination = FluentUnsignedLong
          .of(getUpperBoundRate().multiplyCeilOrZero(sourceAmount))
          .minusOrZero(UnsignedLong.ONE) // <-- Don't let this value go negative.
          .getValue();
        return DeliveredExchangeRateBound.builder()
          .lowEndEstimate(lowEndDestination)
          .highEndEstimate(highEndDestination)
          .build();
      });
  }

  /**
   * Estimate the source amount that delivers the given destination amount.
   * <ol>
   *   <li>Low-end estimate: (may under-deliver, won't over-deliver): lowest source amount
   *     that *may* deliver the given destination amount, if the rate hasn't fluctuated.</li>
   *   <li>High-end estimate: (won't under-deliver, may over-deliver): lowest source amount that
   *     delivers at least the given destination amount, if the rate hasn't fluctuated.</li>
   * </ol>
   */
  public DeliveredExchangeRateBound estimateSourceAmount(final UnsignedLong destinationAmount) {
    Objects.requireNonNull(destinationAmount);
    throw new RuntimeException("Not yet implemented");
  }

  /**
   * Accessor for the lower rate bound.
   *
   * @return A {@link Ratio}.
   */
  public Ratio getLowerBoundRate() {
    if (lowerBoundRate.get() == null) {
      throw new StreamPayerException("No lowerBoundRate was detected from the receiver", SendState.RateProbeFailed);
    }
    return lowerBoundRate.get();
  }

  /**
   * Accessor for the upper rate bound.
   *
   * @return A {@link Ratio}.
   */
  public Ratio getUpperBoundRate() {
    if (upperBoundRate.get() == null) {
      throw new StreamPayerException("No upperBoundRate was detected from the receiver", SendState.RateProbeFailed);
    }
    return upperBoundRate.get();
  }

  /**
   * Helper method to allow the lower and upper rate bounds to be mocked without actually discovering them.
   *
   * @param lowerBoundRate A {@link Ratio} representing the lower rate-bound.
   * @param upperBoundRate A {@link Ratio} representing the upper rate-bound.
   */
  @VisibleForTesting
  protected void setRateBounds(
    final Ratio lowerBoundRate, final Ratio upperBoundRate
  ) {
    this.lowerBoundRate.set(lowerBoundRate);
    this.upperBoundRate.set(upperBoundRate);
  }

  /**
   * Compute whether the exchange rate should be reset.
   *
   * @param packetLowerBoundRate   A {@link Ratio} representing the lower-bound rate probed on the payment path.
   * @param packetUpperBoundRate   A {@link Ratio} representing the upper-bound rate probed on the payment path.
   * @param previousReceivedAmount An {@link UnsignedLong} representing the previously received amount.
   * @param receivedAmount         An {@link UnsignedLong} representing the newly received amount.
   *
   * @return {@code true} if the previous amount is equal to the received amount; or if the upper-bound rate becomes
   *   less-than the lower bound; of if the upper-bound rate becomes less than the lower-bound rate; {@code false}
   *   otherwise.
   */
  @VisibleForTesting
  boolean shouldResetExchangeRate(
    final Ratio packetLowerBoundRate,
    final Ratio packetUpperBoundRate,
    final UnsignedLong previousReceivedAmount,
    final UnsignedLong receivedAmount
  ) {
    return (FluentCompareTo.is(previousReceivedAmount).equalTo(receivedAmount) ||
      FluentCompareTo.is(packetUpperBoundRate).lessThanOrEqualTo(this.lowerBoundRate.get()) ||
      FluentCompareTo.is(packetLowerBoundRate).greaterThanEqualTo(this.upperBoundRate.get()));
  }

  /**
   * Accessor for the received amounts.
   *
   * @return A {@link Map}.
   */
  @VisibleForTesting
  Map<UnsignedLong, UnsignedLong> getReceivedAmounts() {
    return this.receivedAmounts;
  }

  /**
   * Accessor for the received amounts.
   *
   * @return A {@link Map}.
   */
  @VisibleForTesting
  Map<UnsignedLong, UnsignedLong> getSentAmounts() {
    return this.sentAmounts;
  }
}
