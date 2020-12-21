package org.interledger.stream.pay.trackers;

import org.interledger.core.fluent.FluentBigInteger;
import org.interledger.core.fluent.FluentCompareTo;
import org.interledger.core.fluent.Ratio;
import org.interledger.stream.pay.exceptions.StreamPayerException;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.probing.model.EstimatedPaymentOutcome;
import org.interledger.stream.pay.probing.model.PaymentTargetConditions;
import org.interledger.stream.pay.probing.model.PaymentTargetConditions.PaymentType;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedLong;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tracks the amounts that should be sent and delivered.
 */
public class AmountTracker {

  /**
   * Conditions that must be met for the payment to complete, and parameters of its execution.
   */
  private final AtomicReference<PaymentTargetConditions> paymentTargetConditionsAtomicReference;

  /**
   * Total amount sent and fulfilled, in scaled units of the sending account.
   */
  private AtomicReference<BigInteger> amountSentInSourceUnitsRef;

  /**
   * Total amount delivered and fulfilled, in scaled units of the receiving account.
   */
  private AtomicReference<BigInteger> amountDeliveredInDestinationUnitsRef;

  /**
   * Amount sent that is yet to be fulfilled or rejected, in scaled units of the sending account.
   */
  private AtomicReference<BigInteger> sourceAmountInFlightRef;

  /**
   * Estimate of the amount that may be delivered from in-flight packets, in scaled units of the receiving account.
   */
  private AtomicReference<BigInteger> destinationAmountInFlightRef;

  /**
   * Amount in destination units allowed to be lost to rounding, below the enforced exchange rate.
   */
  private final AtomicReference<BigInteger> availableDeliveryShortfallRef;

  /**
   * Maximum amount the recipient can receive on the default stream.
   */
  private final AtomicReference<Optional<UnsignedLong>> remoteReceivedMaxRef;

  /**
   * Should the connection be closed because the receiver violated the STREAM protocol.
   */
  private final AtomicBoolean encounteredProtocolViolation;

  private final ExchangeRateTracker exchangeRateTracker;

  /**
   * No-args Constructor.
   *
   * @param exchangeRateTracker A {@link ExchangeRateTracker}.
   */
  public AmountTracker(final ExchangeRateTracker exchangeRateTracker) {
    this(
      exchangeRateTracker,
      new AtomicReference<>(),
      new AtomicReference<>(BigInteger.ZERO),
      new AtomicReference<>(BigInteger.ZERO),
      new AtomicReference<>(BigInteger.ZERO),
      new AtomicReference<>(BigInteger.ZERO),
      new AtomicReference<>(BigInteger.ZERO),
      new AtomicReference<>(Optional.empty()),
      new AtomicBoolean()
    );
  }

  /**
   * Constructor for test-purposes only.
   */
  @VisibleForTesting
  protected AmountTracker(
    final ExchangeRateTracker exchangeRateTracker,
    final AtomicReference<PaymentTargetConditions> paymentTargetConditionsAtomicReference,
    final AtomicReference<BigInteger> amountSentInSourceUnitsRef,
    final AtomicReference<BigInteger> amountDeliveredInDestinationUnitsRef,
    final AtomicReference<BigInteger> sourceAmountInFlightRef,
    final AtomicReference<BigInteger> destinationAmountInFlightRef,
    final AtomicReference<BigInteger> availableDeliveryShortfallRef,
    final AtomicReference<Optional<UnsignedLong>> remoteReceivedMaxRef,
    final AtomicBoolean encounteredProtocolViolation
  ) {
    this.exchangeRateTracker = Objects.requireNonNull(exchangeRateTracker);

    this.paymentTargetConditionsAtomicReference = Objects.requireNonNull(paymentTargetConditionsAtomicReference);
    this.amountSentInSourceUnitsRef = Objects.requireNonNull(amountSentInSourceUnitsRef);
    this.amountDeliveredInDestinationUnitsRef = Objects.requireNonNull(amountDeliveredInDestinationUnitsRef);
    this.sourceAmountInFlightRef = Objects.requireNonNull(sourceAmountInFlightRef);
    this.destinationAmountInFlightRef = Objects.requireNonNull(destinationAmountInFlightRef);
    this.availableDeliveryShortfallRef = Objects.requireNonNull(availableDeliveryShortfallRef);
    this.remoteReceivedMaxRef = Objects.requireNonNull(remoteReceivedMaxRef);
    this.encounteredProtocolViolation = Objects.requireNonNull(encounteredProtocolViolation);
  }

  /**
   * Set the payment target details for this payment, and yield a {@link EstimatedPaymentOutcome}.
   *
   * @param paymentType           A {@link PaymentType}.
   * @param minExchangeRate       A {@link BigDecimal} representing the minimum acceptable exchange-rate as indicated by
   *                              the sender or the system.
   * @param maxSourcePacketAmount An {@link UnsignedLong} representing the maximum packet amount that the payment path
   *                              will accept.
   * @param targetAmount          A {@link BigInteger} representing the target amount that we expect to send.
   *
   * @return A {@link EstimatedPaymentOutcome}.
   */
  // TODO: Unit Test
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

    if (!lowerBoundRate.isPositive() || !upperBoundRate.isPositive()) {
      final String errorMessage = String.format(
        "Rate Probe discovered invalid exchange rates. lowerBoundRate=%s upperBoundRate=%s",
        lowerBoundRate, upperBoundRate
      );
      throw new StreamPayerException(errorMessage, SendState.InsufficientExchangeRate);
    }

    // Ensure the exchange-rate is valid.
    final Ratio minExchangeRateRatio = Ratio.from(minExchangeRate);
    this.validateExchangeRates(lowerBoundRate, minExchangeRateRatio);
    this.validateMinPacketAmount(
      lowerBoundRate, minExchangeRateRatio, maxSourcePacketAmount
    );

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

  /**
   * Indicate if a STREAM or other protocol violation has been encountered.
   *
   * @return {@code true} if a violation has occurred; {@code false} otherwise.
   */
  public boolean encounteredProtocolViolation() {
    return encounteredProtocolViolation.get();
  }

  /**
   * As the attributes of a payment path are discovered, a {@link PaymentTargetConditions} will be assiged for future
   * inspection.
   *
   * @return An optionally-present {@link PaymentTargetConditions}.
   */
  public Optional<PaymentTargetConditions> getPaymentTargetConditions() {
    return Optional.ofNullable(this.paymentTargetConditionsAtomicReference.get());
  }

  public Optional<UnsignedLong> getRemoteReceivedMax() {
    return remoteReceivedMaxRef.get();
  }

  /**
   * Total amount sent and fulfilled, in scaled units of the sending account.
   *
   * @return A {@link BigInteger}.
   */
  public BigInteger getAmountSentInSourceUnits() {
    return amountSentInSourceUnitsRef.get();
  }

  /**
   * Total amount delivered and fulfilled, in scaled units of the receiving account.
   *
   * @return A {@link BigInteger}.
   */
  public BigInteger getAmountDeliveredInDestinationUnits() {
    return amountDeliveredInDestinationUnitsRef.get();
  }

  /**
   * The total source amount in-flight, in sender's units.
   *
   * @return A {@link BigInteger}.
   */
  public BigInteger getSourceAmountInFlight() {
    return sourceAmountInFlightRef.get();
  }

  /**
   * The total destination amount in flight, in destination units.
   *
   * @return A {@link BigInteger}.
   */
  public BigInteger getDestinationAmountInFlight() {
    return destinationAmountInFlightRef.get();
  }

  /**
   * The amount of shortfall in delivered units (i.e., the number of units more that would be required to actually
   * deliver any value).
   *
   * @return A {@link BigInteger}.
   */
  public BigInteger getAvailableDeliveryShortfall() {
    return this.availableDeliveryShortfallRef.get();
  }

  /**
   * Add {@code amountToAdd} to the overall number of source units that are currently in-flight.
   *
   * @param amountToAdd An {@link UnsignedLong}.
   */
  public void addToSourceAmountInFlight(final UnsignedLong amountToAdd) {
    Objects.requireNonNull(amountToAdd);
    this.sourceAmountInFlightRef.getAndAccumulate(amountToAdd.bigIntegerValue(), BigInteger::add);
  }

  /**
   * Add {@code amountToAdd} to the overall number of destination units that are currently in-flight.
   *
   * @param amountToAdd An {@link UnsignedLong}.
   */
  public void addToDestinationAmountInFlight(final UnsignedLong amountToAdd) {
    Objects.requireNonNull(amountToAdd);
    this.destinationAmountInFlightRef.getAndAccumulate(amountToAdd.bigIntegerValue(), BigInteger::add);
  }

  /**
   * Subtract {@code amountToSubtract} from the overall number of source units that are currently in-flight.
   *
   * @param amountToSubtract An {@link UnsignedLong}.
   */
  public void subtractFromSourceAmountInFlight(final UnsignedLong amountToSubtract) {
    Objects.requireNonNull(amountToSubtract);
    this.sourceAmountInFlightRef.getAndAccumulate(amountToSubtract.bigIntegerValue(), BigInteger::subtract);
  }

  /**
   * Subtract {@code amountToSubtract} from the overall number of destination units that are currently in-flight.
   *
   * @param amountToSubtract An {@link UnsignedLong}.
   */
  public void subtractFromDestinationAmountInFlight(final UnsignedLong amountToSubtract) {
    Objects.requireNonNull(amountToSubtract);
    this.destinationAmountInFlightRef.getAndAccumulate(amountToSubtract.bigIntegerValue(), BigInteger::subtract);
  }

  /**
   * Reduce the delivery shortfall.
   *
   * @param amountToReduce An {@link UnsignedLong}.
   */
  public void reduceDeliveryShortfall(final UnsignedLong amountToReduce) {
    Objects.requireNonNull(amountToReduce);
    this.availableDeliveryShortfallRef.getAndAccumulate(amountToReduce.bigIntegerValue(), BigInteger::subtract);
  }

  /**
   * Increase the delivery shortfall.
   *
   * @param amountToIncrease An {@link UnsignedLong}.
   */
  public void increaseDeliveryShortfall(final UnsignedLong amountToIncrease) {
    Objects.requireNonNull(amountToIncrease);
    this.availableDeliveryShortfallRef.getAndAccumulate(amountToIncrease.bigIntegerValue(), BigInteger::add);
  }

  /**
   * Indicate that a protocol violation was encountered.
   */
  public void setEncounteredProtocolViolation() {
    this.encounteredProtocolViolation.set(true);
  }

  /**
   * Increase the amount sent by {@code sourceAmount}.
   *
   * @param sourceAmount An {@link UnsignedLong}.
   */
  public void addAmountSent(final UnsignedLong sourceAmount) {
    Objects.requireNonNull(sourceAmount);
    this.amountSentInSourceUnitsRef.getAndAccumulate(sourceAmount.bigIntegerValue(), BigInteger::add);
  }

  /**
   * Add {@code destinationAmount} to the overall tracked total for the destination amount delivered.
   *
   * @param destinationAmount An {@link UnsignedLong}.
   */
  public void addAmountDelivered(final UnsignedLong destinationAmount) {
    Objects.requireNonNull(destinationAmount);
    this.amountDeliveredInDestinationUnitsRef.getAndAccumulate(destinationAmount.bigIntegerValue(), BigInteger::add);
  }

  /**
   * Update the remote-max to {@code remoteMax}.
   *
   * @param remoteMax An {@link UnsignedLong}.
   */
  public void updateRemoteMax(final UnsignedLong remoteMax) {
    Objects.requireNonNull(remoteMax);
    this.remoteReceivedMaxRef.set(Optional.of(remoteMax));
  }

  /**
   * <p>Validates the discovered vs minimum exchange rates.</p>
   *
   * <p>In order to accommodate 1:1 FX rates with 0 slippage, as well as to accurately mimic ILP payment-path rounding
   * behavior (i.e., the minimum destination amount is rounded up and the destination amount is rounded down, and fails
   * if the minimum is greater than the destination amount), then this method checks to see if the 'floor(probedRate)'
   * is greater-than-or-equal-to the `ceil(minimumRate)`. If this equation returns {@code true}, then the exchange rates
   * are valid. Otherwise, there is an insufficient exchange rate for the packet to complete properly.</p>
   *
   * <p>In other words, in order for the rates to be accepable, one of the following must occur:</p>
   *
   * <ul>
   *   <li>Probed rate >= minimum rate and least one of the rates is an integer, OR</li>
   *   <li>Probed rate > minimum rate and there exists an integer value between the rates.</li>
   * </ul>
   *
   * <p>Some examples that should validate via this method:</p>
   * <ul>
   *   <li>Probed rate is 2 and minimum is 1.9 => (valid)</li>
   *   <li>Probed rate is 2.1 and minimum is 2 => (valid)</li>
   *   <li>Probed rate is 2.1 and minimum is 1.9 => (valid)</li>
   *   <li>Probed rate is 1.5 and minimum is 1.1 => (rounding errors are possible)</li>
   * </ul>
   *
   * @param lowerBoundRate       A {@link Ratio} holding the lower-bound rate detected on the payment path.
   * @param minExchangeRateRatio A {@link Ratio} holding the minimally acceptable exchange rate.
   *
   * @see "https://github.com/interledgerjs/interledgerjs/issues/167"
   */
  @VisibleForTesting
  protected void validateExchangeRates(final Ratio lowerBoundRate, final Ratio minExchangeRateRatio) {
    Objects.requireNonNull(lowerBoundRate);
    Objects.requireNonNull(minExchangeRateRatio);

    final BigInteger floorProbedRate = lowerBoundRate.multiplyFloor(BigInteger.ONE);
    final BigInteger ceilMinExchangeRate = minExchangeRateRatio.multiplyCeil(BigInteger.ONE);

    if (FluentCompareTo.is(floorProbedRate).notGreaterThanEqualTo(ceilMinExchangeRate)) {
      final String errorMessage = String.format(
        "Rate-probed exchange-rate of %s is less-than than the minimum exchange-rate of %s",
        lowerBoundRate.toBigDecimal(), minExchangeRateRatio.toBigDecimal()
      );
      throw new StreamPayerException(errorMessage, SendState.InsufficientExchangeRate);
    }
  }

  /**
   * Packets that aren't at least this minimum source amount *may* fail due to rounding. If the max packet amount is
   * insufficient, fail fast, since the payment is unlikely to succeed.
   *
   * @param lowerBoundRate        A {@link Ratio} holding the lower-bound rate detected on the payment path.
   * @param minExchangeRateRatio  A {@link Ratio} holding the minimally acceptable exchange rate.
   * @param maxSourcePacketAmount An {@link UnsignedLong} representing the largest amount that is valid for a single
   *                              packet.
   */
  @VisibleForTesting
  protected void validateMinPacketAmount(
    final Ratio lowerBoundRate, final Ratio minExchangeRateRatio, final UnsignedLong maxSourcePacketAmount
  ) {
    Objects.requireNonNull(lowerBoundRate);
    Objects.requireNonNull(minExchangeRateRatio);
    Objects.requireNonNull(maxSourcePacketAmount);

    final Ratio marginOfError = lowerBoundRate.subtract(minExchangeRateRatio);
    final UnsignedLong minSourcePacketAmount = FluentBigInteger.of(BigInteger.ONE)
      .timesCeil(marginOfError.reciprocal().orElse(Ratio.ONE)).orMaxUnsignedLong();

    if (FluentCompareTo.is(maxSourcePacketAmount).notGreaterThanEqualTo(minSourcePacketAmount)) {
      final String errorMessage = String.format(
        "Rate enforcement may incur rounding errors. maxPacketAmount=%s is below proposed minimum of %s",
        maxSourcePacketAmount, minSourcePacketAmount
      );
      throw new StreamPayerException(errorMessage, SendState.ExchangeRateRoundingError);
    }
  }
}
