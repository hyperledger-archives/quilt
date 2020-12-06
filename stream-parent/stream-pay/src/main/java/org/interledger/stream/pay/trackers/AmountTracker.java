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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks the amounts that should be sent and delivered.
 */
public class AmountTracker {

  private final static Logger LOGGER = LoggerFactory.getLogger(AmountTracker.class);

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

    // TODO: Ensure in-flight amount are tracked properly

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

    // The rate is insufficient only if the marginOfError is negative. A zero-margin is still valid.
    final Ratio minExchangeRateRatio = Ratio.from(minExchangeRate);
    final Ratio marginOfError = lowerBoundRate.subtract(minExchangeRateRatio);

    // In order to accomadate 1:1 FX rates with 0 slippage then do the following:
    // (See https://github.com/interledgerjs/interledgerjs/issues/167 for more details)
    // 1) If the marginOfError is 0 and the rate is a positive integer, the payment should proceed.
    // 2) Howevever, if the marginOfError is 0 or negative, and the minExchangeRateRatio has any decimal portion then
    // the payment should fail.
    if (marginOfError.isNotPositive()) { // <-- 0 or a negative rate.
      if (marginOfError.isZero() && minExchangeRateRatio.isPositiveInteger()) {
        // Let the payment continue because a 0-margin with a whole FX can succeed.
      } else {
        final String errorMessage = String.format(
          "Rate-probed exchange-rate of %s is less-than than the minimum exchange-rate of %s",
          lowerBoundRate.toBigDecimal(), minExchangeRateRatio.toBigDecimal()
        );
        throw new StreamPayerException(errorMessage, SendState.InsufficientExchangeRate);
      }
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
      .timesCeil(marginOfError.reciprocal().orElse(Ratio.ONE)).orMaxUnsignedLong();
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

//  /**
//   * Sets aside in-flight amounts for the values found in {@code streamPacketRequest}. Note that if an exception is
//   * thrown anywhere in the Stream Pay run-loop, the payment will fail, so no need to do anything with tracked amounts
//   * in that case.
//   *
//   * @param streamPacketRequest
//   */
//  public ComputedStreamPacketAmounts reserveTrackedAmounts(final StreamPacketRequest streamPacketRequest) {
//    Objects.requireNonNull(streamPacketRequest);
//
//    UnsignedLong highEndDestinationAmount = UnsignedLong.ZERO;
//    UnsignedLong deliveryDeficit = UnsignedLong.ZERO;
//
//    if (this.getPaymentTargetConditions().isPresent()) {
//      PaymentTargetConditions target = this.getPaymentTargetConditions().get();
//
//      // Estimate the most that this packet will deliver
//      highEndDestinationAmount = FluentUnsignedLong.of(streamPacketRequest.minDestinationAmount())
//        .orGreater(
//          this.exchangeRateTracker.estimateDestinationAmount(streamPacketRequest.sourceAmount()).highEndEstimate()
//        ).getValue();
//
//      // Update in-flight amounts
//      this.addToSourceAmountInFlight(streamPacketRequest.sourceAmount());
//      this.addToDestinationAmountInFlight(highEndDestinationAmount);
//
//      // Update the delivery shortfall, if applicable
//      final UnsignedLong baselineMinDestinationAmount = FluentUnsignedLong
//        .of(streamPacketRequest.sourceAmount())
//        .timesCeil(Ratio.from(target.minExchangeRate()))
//        .getValue();
//      deliveryDeficit = baselineMinDestinationAmount.minus(streamPacketRequest.minDestinationAmount());
//      if (FluentUnsignedLong.of(deliveryDeficit).isPositive()) {
//        this.reduceDeliveryShortfall(deliveryDeficit);
//      }
//    }
//
//    return ComputedStreamPacketAmounts.builder()
//      .highEndDestinationAmount(highEndDestinationAmount)
//      .deliveryDeficit(deliveryDeficit)
//      .build();
//  }
//
//  /**
//   * Executed after a stream-packet is processed. Note that if an exception is thrown anywhere in the Stream Pay
//   * run-loop, the payment will fail, so no need to do anything with tracked amounts in that case.
//   *
//   * @param streamPacketRequest
//   * @param computedStreamPacketAmounts
//   * @param streamPacketReply
//   */
//  public void commitTrackedAmounts(
//    final StreamPacketRequest streamPacketRequest,
//    final ComputedStreamPacketAmounts computedStreamPacketAmounts,
//    final StreamPacketReply streamPacketReply
//  ) {
//    Objects.requireNonNull(streamPacketRequest);
//    Objects.requireNonNull(computedStreamPacketAmounts);
//    Objects.requireNonNull(streamPacketReply);
//
//    Optional<UnsignedLong> destinationAmount = streamPacketReply.destinationAmountClaimed();
//    if (streamPacketReply.isFulfill()) {
//      // Delivered amount must be *at least* the minimum acceptable amount we told the receiver
//      // No matter what, since they fulfilled it, we must assume they got at least the minimum
//      if (!streamPacketReply.destinationAmountClaimed().isPresent()) {
//        // Technically, an intermediary could strip the data so we can't ascertain whose fault this is
//        LOGGER.warn("Ending payment: packet fulfilled with no authentic STREAM data");
//        destinationAmount = Optional.of(streamPacketRequest.minDestinationAmount());
//        this.setEncounteredProtocolViolation();
//      } else if (destinationAmount.isPresent() && FluentCompareTo.is(destinationAmount.get())
//        .lessThan(streamPacketRequest.minDestinationAmount())) {
//        if (LOGGER.isWarnEnabled()) {
//          LOGGER.warn(
//            "Ending payment: receiver violated protocol. packet below minimum exchange rate was fulfilled. "
//              + "destinationAmount={}  minDestinationAmount={}",
//            destinationAmount,
//            streamPacketRequest.minDestinationAmount()
//          );
//        }
//        destinationAmount = Optional.of(streamPacketRequest.minDestinationAmount());
//        this.setEncounteredProtocolViolation();
//      } else {
//        if (LOGGER.isTraceEnabled()) {
//          LOGGER.trace(
//            "Packet sent: sourceAmount={}  destinationAmount={} minDestinationAmount={}",
//            streamPacketRequest.sourceAmount(), destinationAmount, streamPacketRequest.minDestinationAmount()
//          );
//        }
//      }
//
//      this.addAmountSent(streamPacketRequest.sourceAmount());
//      this.addAmountDelivered(destinationAmount);
//    } else if (destinationAmount.isPresent() && FluentCompareTo.is(destinationAmount.get())
//      .lessThan(streamPacketRequest.minDestinationAmount())) {
//      if (LOGGER.isDebugEnabled()) {
//        LOGGER.debug(
//          "Packet rejected for insufficient rate: minDestinationAmount={} destinationAmount={}",
//          streamPacketRequest.minDestinationAmount(), destinationAmount
//        );
//      }
//    }
//
//    this.subtractFromSourceAmountInFlight(streamPacketRequest.sourceAmount());
//    this.subtractFromDestinationAmountInFlight(computedStreamPacketAmounts.highEndDestinationAmount());
//
//    // If this packet failed, "refund" the delivery deficit so it may be retried
//    if (FluentUnsignedLong.of(computedStreamPacketAmounts.deliveryDeficit()).isPositive() && streamPacketReply
//      .isReject()) {
//      this.increaseDeliveryShortfall(computedStreamPacketAmounts.deliveryDeficit());
//    }
//
//    this.getPaymentTargetConditions().ifPresent(target -> {
//      if (target.paymentType() == PaymentType.FIXED_SEND) {
//        if (LOGGER.isTraceEnabled()) {
//          LOGGER.trace(
//            "Fixed Send Payment has sent {} of {}, {} in-flight",
//            this.getAmountSentInSourceUnits(),
//            target.maxSourceAmount(),
//            this.getSourceAmountInFlight()
//          );
//        }
//      } else if (target.paymentType() == PaymentType.FIXED_DELIVERY) {
//        if (LOGGER.isTraceEnabled()) {
//          LOGGER.trace(
//            "Fixed Delivery Payment has sent {} of {}, {} in-flight",
//            this.getAmountDeliveredInDestinationUnits(),
//            target.minDeliveryAmount(),
//            this.getDestinationAmountInFlight()
//          );
//        }
//      }
//    });
//  }
//
//  /**
//   * An interstitial object for holding return values for amount tracking.
//   */
//  @Immutable
//  public interface ComputedStreamPacketAmounts {
//
//    static ImmutableComputedStreamPacketAmounts.Builder builder() {
//      return ImmutableComputedStreamPacketAmounts.builder();
//    }
//
//    /**
//     * The computed (high-end) estimate of the amount that will be delivered by a particular stream packet.
//     *
//     * @return An {@link UnsignedLong}.
//     */
//    @Default
//    default UnsignedLong highEndDestinationAmount() {
//      return UnsignedLong.ZERO;
//    }
//
//    /**
//     * A delivery deficit, if any, which is the micro-amount allowed for the final packet to not send (because of
//     * rounding errors) and have the payment still complete.
//     *
//     * @return An {@link UnsignedLong}.
//     */
//    @Default
//    default UnsignedLong deliveryDeficit() {
//      return UnsignedLong.ZERO;
//    }
//  }
}
