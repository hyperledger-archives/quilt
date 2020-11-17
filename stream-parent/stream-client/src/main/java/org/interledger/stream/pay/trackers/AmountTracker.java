package org.interledger.stream.pay.trackers;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedLong;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.interledger.core.fluent.FluentBigInteger;
import org.interledger.core.fluent.FluentCompareTo;
import org.interledger.core.fluent.Ratio;
import org.interledger.stream.pay.exceptions.StreamPayerException;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.probing.model.EstimatedPaymentOutcome;
import org.interledger.stream.pay.probing.model.PaymentTargetConditions;
import org.interledger.stream.pay.probing.model.PaymentTargetConditions.PaymentType;

/**
 * Tracks the amounts that should be sent and delivered.
 */
public class AmountTracker {

//  private final static Logger LOGGER = LoggerFactory.getLogger(AmountTracker.class);

  /**
   * Conditions that must be met for the payment to complete, and parameters of its execution.
   */
  private final AtomicReference<PaymentTargetConditions> paymentTargetConditionsAtomicReference;

  /**
   * Total amount sent and fulfilled, in scaled units of the sending account
   */
  private AtomicReference<BigInteger> amountSentInSourceUnitsRef = new AtomicReference<>(BigInteger.ZERO);

  /**
   * Total amount delivered and fulfilled, in scaled units of the receiving account
   */
  private AtomicReference<BigInteger> amountDeliveredInDestinationUnitsRef = new AtomicReference<>(BigInteger.ZERO);

  /**
   * Amount sent that is yet to be fulfilled or rejected, in scaled units of the sending account
   */
  private AtomicReference<BigInteger> sourceAmountInFlightRef = new AtomicReference<>(BigInteger.ZERO);

  /**
   * Estimate of the amount that may be delivered from in-flight packets, in scaled units of the receiving account
   */
  private AtomicReference<BigInteger> destinationAmountInFlightRef = new AtomicReference<>(BigInteger.ZERO);

  /**
   * Amount in destination units allowed to be lost to rounding, below the enforced exchange rate.
   */
  private final AtomicReference<BigInteger> availableDeliveryShortfallRef = new AtomicReference<>(BigInteger.ZERO);

  /**
   * Maximum amount the recipient can receive on the default stream
   */
  private final AtomicReference<Optional<UnsignedLong>> remoteReceivedMaxRef = new AtomicReference<>(Optional.empty());

  /**
   * Should the connection be closed because the receiver violated the STREAM protocol?
   */
  private final AtomicBoolean encounteredProtocolViolation = new AtomicBoolean();

  private final ExchangeRateTracker exchangeRateTracker;

  public AmountTracker(ExchangeRateTracker exchangeRateTracker) {
    this.exchangeRateTracker = exchangeRateTracker;
    this.paymentTargetConditionsAtomicReference = new AtomicReference<>();
  }

  // TODO: Javadoc
  public EstimatedPaymentOutcome setPaymentTarget(
    final PaymentType paymentType, // Unused but placeholder for Invoices.
    final BigDecimal minExchangeRate,
    final UnsignedLong maxSourcePacketAmount,
    // TODO: Consider ScaledAmount?
    final BigInteger targetAmount // Scaled correctly for the source account.
  ) {
    Objects.requireNonNull(minExchangeRate);
    Objects.requireNonNull(maxSourcePacketAmount);
    Objects.requireNonNull(targetAmount);
    Preconditions.checkState(targetAmount.compareTo(BigInteger.ZERO) > 0, "targetAmount must be greater-than 0");

    // There must be a rate in the FX tracker or this call will fail with an exception.
    Ratio lowerBoundRate = this.exchangeRateTracker.getLowerBoundRate();
    Ratio upperBoundRate = this.exchangeRateTracker.getUpperBoundRate();

    // TODO: ScaledRates are only allowed to be positive, so this check can go away.
    if (!lowerBoundRate.isPositive() || !upperBoundRate.isPositive()) {
      final String errorMessage = String.format(
        "Rate Probe discovered invalid exchange rates. lowerBoundRate=%s upperBoundRate=%s",
        lowerBoundRate, upperBoundRate
      );
      throw new StreamPayerException(errorMessage, SendState.InsufficientExchangeRate);
    }

    final Ratio minExchangeRateRatio = Ratio.from(minExchangeRate);
    final Ratio marginOfError = lowerBoundRate.subtract(minExchangeRateRatio);
    if (!marginOfError.isPositive()) {
      final String errorMessage = String.format(
        "Rate Probed exchange rate of %s is not greater than minimum of %s", lowerBoundRate, minExchangeRate
      );
      throw new StreamPayerException(errorMessage, SendState.InsufficientExchangeRate);
    }

    // Assuming we accurately know the real exchange rate, if the actual destination amount is less than the
    // min destination amount set by the sender, the packet fails due to a rounding error,
    // since intermediaries round down, but senders round up:
    // - realDestinationAmount = floor(sourceAmount * realExchangeRate) --- Determined by intermediaries
    // - minDestinationAmount  =  ceil(sourceAmount * minExchangeRate)  --- Determined by sender

    // Packets that aren't at least this minimum source amount *may* fail due to rounding.
    // If the max packet amount is insufficient, fail fast, since the payment is unlikely to succeed.
    final UnsignedLong minSourcePacketAmount = FluentBigInteger.of(BigInteger.ONE)
      // per the checks above, reciprocal will be present.
      .timesCeil(marginOfError.reciprocal().get()).orMaxUnsignedLong();
    if (FluentCompareTo.is(maxSourcePacketAmount).notGreaterThanEqualTo(minSourcePacketAmount)) {
      final String errorMessage = String.format(
        "Rate enforcement may incur rounding errors. maxPacketAmount=%s is below proposed minimum of %s",
        maxSourcePacketAmount, minSourcePacketAmount
      );
      // TODO: Should be ConnectorError once the max-packet tracker is working properly.
      throw new StreamPayerException(errorMessage, SendState.ExchangeRateRoundingError);
    }

    // To prevent the final packet from failing due to rounding, account for a small "shortfall" of 1 source unit,
    // converted to destination units, to tolerate below the enforced destination amounts from the minimum exchange
    // rate.
    this.availableDeliveryShortfallRef.set(
      FluentBigInteger.of(BigInteger.ONE).timesCeil(minExchangeRate).getValue()
    );

    if (paymentType == PaymentType.FIXED_SEND) {
      final BigInteger estimatedNumberOfPackets = FluentBigInteger.of(targetAmount)
        .divideCeil(maxSourcePacketAmount.bigIntegerValue()).getValue();
      final BigInteger maxSourceAmount = targetAmount; // TODO: Is this correct? Seems too high.
      final BigInteger minDeliveryAmount =
        // TODO: is Ceil correct here?
        FluentBigInteger.of(targetAmount.subtract(BigInteger.ONE)).timesCeil(minExchangeRate).getValue();

      this.paymentTargetConditionsAtomicReference.set(PaymentTargetConditions.builder()
        .paymentType(PaymentType.FIXED_SEND)
        .minDeliveryAmount(minDeliveryAmount)
        .maxSourceAmount(maxSourceAmount)
        .minExchangeRate(minExchangeRate)
        .build());

      return EstimatedPaymentOutcome.builder()
        .maxSendAmountInWholeSourceUnits(maxSourceAmount)
        .minDeliveryAmountInWholeDestinationUnits(minDeliveryAmount)
        .estimatedNumberOfPackets(estimatedNumberOfPackets)
        .build();

    } else if (paymentType == PaymentType.FIXED_DELIVERY) {
//      if (!minExchangeRate.isPositive()) {
//        throw new StreamPayerException(
//          "quote failed: unenforceable payment delivery. min exchange rate is 0", SendState.UnenforceableDelivery
//        );
//      }

      // The final packet may be less than the minimum source packet amount, but if the minimum rate is enforced,
      // it would fail due to rounding. To account for this, increase max source amount by 1 unit.
      final BigInteger maxSourceAmount = FluentBigInteger.of(targetAmount)
        // reciprocal
        .timesCeil(BigDecimal.ONE.divide(minExchangeRate, MathContext.DECIMAL64)).getValue()
        .add(BigInteger.ONE);

      final BigInteger minDeliveryAmount = targetAmount;
      final BigInteger estimatedNumberOfPackets = FluentBigInteger.of(maxSourceAmount)
        .divideCeil(maxSourcePacketAmount.bigIntegerValue()).getValue();

      this.paymentTargetConditionsAtomicReference.set(PaymentTargetConditions.builder()
        .paymentType(PaymentType.FIXED_DELIVERY)
        .minDeliveryAmount(minDeliveryAmount)
        .maxSourceAmount(maxSourceAmount)
        .minExchangeRate(minExchangeRate)
        .build());

      return EstimatedPaymentOutcome.builder()
        .maxSendAmountInWholeSourceUnits(maxSourceAmount)
        .minDeliveryAmountInWholeDestinationUnits(minDeliveryAmount)
        .estimatedNumberOfPackets(estimatedNumberOfPackets)
        .build();
    } else {
      throw new StreamPayerException(
        String.format("Unhandled PaymentType. paymentType=%s", paymentType),
        SendState.RateProbeFailed
      );
    }
  }

  public boolean encounteredProtocolViolation() {
    return encounteredProtocolViolation.get();
  }

  public Optional<PaymentTargetConditions> getPaymentTargetConditions() {
    return Optional.ofNullable(this.paymentTargetConditionsAtomicReference.get());
  }

  public Optional<UnsignedLong> getRemoteReceivedMax() {
    return remoteReceivedMaxRef.get();
  }

  public BigInteger getAmountSentInSourceUnits() {
    return amountSentInSourceUnitsRef.get();
  }

  public BigInteger getAmountDeliveredInDestinationUnits() {
    return amountDeliveredInDestinationUnitsRef.get();
  }

  public BigInteger getSourceAmountInFlight() {
    return sourceAmountInFlightRef.get();
  }

  public BigInteger getDestinationAmountInFlight() {
    return destinationAmountInFlightRef.get();
  }

  public BigInteger getAvailableDeliveryShortfall() {
    return this.availableDeliveryShortfallRef.get();
  }

  // TODO: Unit-test with threads to validate this is safe.
  public void addToSourceAmountInFlight(final UnsignedLong amountToAdd) {
    Objects.requireNonNull(amountToAdd);
    this.sourceAmountInFlightRef.getAndAccumulate(amountToAdd.bigIntegerValue(), BigInteger::add);
  }

  // TODO: Unit-test with threads to validate this is safe.
  public void addToDestinationAmountInFlight(final UnsignedLong amountToAdd) {
    Objects.requireNonNull(amountToAdd);
    this.destinationAmountInFlightRef.getAndAccumulate(amountToAdd.bigIntegerValue(), BigInteger::add);
  }

  // TODO: Unit-test with threads to validate this is safe.
  public void subtractFromSourceAmountInFlight(final UnsignedLong amountToSubtract) {
    Objects.requireNonNull(amountToSubtract);
    this.sourceAmountInFlightRef.getAndAccumulate(amountToSubtract.bigIntegerValue(), BigInteger::subtract);
  }

  // TODO: Unit-test with threads to validate this is safe.
  public void subtractFromDestinationAmountInFlight(final UnsignedLong amountToSubtract) {
    Objects.requireNonNull(amountToSubtract);
    this.destinationAmountInFlightRef.getAndAccumulate(amountToSubtract.bigIntegerValue(), BigInteger::subtract);
  }

  // TODO: Unit-test with threads to validate this is safe.
  public void reduceDeliveryShortfall(final UnsignedLong amountToReduce) {
    Objects.requireNonNull(amountToReduce);
    this.availableDeliveryShortfallRef.getAndAccumulate(amountToReduce.bigIntegerValue(), BigInteger::subtract);
  }

  // TODO: Unit-test with threads to validate this is safe.
  public void increaseDeliveryShortfall(final UnsignedLong amountToIncrease) {
    Objects.requireNonNull(amountToIncrease);
    this.availableDeliveryShortfallRef.getAndAccumulate(amountToIncrease.bigIntegerValue(), BigInteger::add);
  }

  public void setEncounteredProtocolViolation() {
    this.encounteredProtocolViolation.set(true);
  }

  public void addAmountSent(final UnsignedLong sourceAmount) {
    Objects.requireNonNull(sourceAmount);
    this.amountSentInSourceUnitsRef.getAndAccumulate(sourceAmount.bigIntegerValue(), BigInteger::add);
  }

  public void addAmountDelivered(final Optional<UnsignedLong> destinationAmount) {
    Objects.requireNonNull(destinationAmount);
    destinationAmount.ifPresent(amount -> {
      this.amountDeliveredInDestinationUnitsRef.getAndAccumulate(amount.bigIntegerValue(), BigInteger::add);
    });
  }

  public void updateRemoteMax(final UnsignedLong remoteMax) {
    Objects.requireNonNull(remoteMax);
    this.remoteReceivedMaxRef.set(Optional.of(remoteMax));
  }
}
