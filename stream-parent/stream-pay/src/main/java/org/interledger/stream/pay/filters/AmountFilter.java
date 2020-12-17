package org.interledger.stream.pay.filters;

import org.interledger.core.fluent.FluentBigInteger;
import org.interledger.core.fluent.FluentCompareTo;
import org.interledger.core.fluent.FluentUnsignedLong;
import org.interledger.core.fluent.Ratio;
import org.interledger.stream.StreamPacketUtils;
import org.interledger.stream.frames.ErrorCodes;
import org.interledger.stream.frames.StreamMoneyFrame;
import org.interledger.stream.pay.filters.chain.StreamPacketFilterChain;
import org.interledger.stream.pay.model.ModifiableStreamPacketRequest;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.model.StreamPacketReply;
import org.interledger.stream.pay.model.StreamPacketRequest;
import org.interledger.stream.pay.probing.model.PaymentTargetConditions;
import org.interledger.stream.pay.probing.model.PaymentTargetConditions.PaymentType;
import org.interledger.stream.pay.trackers.AmountTracker;
import org.interledger.stream.pay.trackers.ExchangeRateTracker;
import org.interledger.stream.pay.trackers.PaymentSharedStateTracker;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;

/**
 * Tracks and calculates amounts to send and deliver.
 */
public class AmountFilter implements StreamPacketFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(AmountFilter.class);
  private static final boolean NO_MORE_TO_DELIVER = false;

  private final PaymentSharedStateTracker paymentSharedStateTracker;

  public AmountFilter(final PaymentSharedStateTracker paymentSharedStateTracker) {
    this.paymentSharedStateTracker = Objects.requireNonNull(paymentSharedStateTracker);
  }

  @Override
  public SendState nextState(final ModifiableStreamPacketRequest streamPacketRequest) {
    Objects.requireNonNull(streamPacketRequest);

    final AmountTracker amountTracker = paymentSharedStateTracker.getAmountTracker();

    if (amountTracker.encounteredProtocolViolation()) {
      streamPacketRequest.setStreamErrorCodeForConnectionClose(ErrorCodes.ProtocolViolation);
      return SendState.ReceiverProtocolViolation;
    }

    // If there are no PaymentTargetConditions, then always return READY (ergo, this `map` is correct).
    return amountTracker.getPaymentTargetConditions()
      .map(target -> {

        // Check ReceiveMax vs SendMin.
        if (this.checkForIncompatibleReceiveMax(amountTracker, target)) {
          LOGGER.error(String.format(
            "Ending payment: minimum delivery amount is too much for recipient. minDeliveryAmount={} remoteReceiveMax={}",
            target.minDeliveryAmount(), amountTracker.getRemoteReceivedMax()
          ));
          streamPacketRequest.setStreamErrorCodeForConnectionClose(ErrorCodes.ApplicationError);
          return SendState.IncompatibleReceiveMax;
        }

        // Check PaidFixedSend
        if (this.checkIfFixedSendPaymentIsComplete(amountTracker, target)) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
              "Payment complete: paid fixed source amount. sent {}",
              amountTracker.getAmountSentInSourceUnits()
            );
          }
          streamPacketRequest.setStreamErrorCodeForConnectionClose(ErrorCodes.NoError);
          return SendState.End;
        }

        // Ensure we never overpay the maximum source amount. Note that this only works because this evaluation is
        // performed as part of the `nextState` operations, which are always run on a single-thread in the RunLoop.
        // Thus, this calculation is always accurate (i.e., there is no other thread that could be mutating the
        // available send to be higher than it should be because multi-threaded behavior is only ever reducing the
        // AmountSentInSourceUnits, never increasing it). Note that if there is nothing availableToSend, and there is
        // nothing in-flight, then it means we should end the payment because we won't schedule any more packets.
        final BigInteger availableToSend = this.computeAmountAvailableToSend(target);
        final boolean anyInFlight = FluentBigInteger.of(amountTracker.getSourceAmountInFlight()).isPositive();
        final boolean anyAvailableToSend = FluentBigInteger.of(availableToSend).isPositive(); // <- 1 or more.

        if (anyAvailableToSend) {
          // Do nothing. There's more to send, so send it.
        } else { // <-- anyAvailableToSend is false if we get here.
          if (anyInFlight) {
            // No more to send but in-flight could reject and become available; so Wait just to be sure.
            return SendState.Wait;
          } else {
            // No more to send; nothing in-flight to become available; so nothing more will ever send, so End.
            return SendState.End;
          }
        }

        // Compute source amount (always positive)
        final UnsignedLong maxPacketAmount = paymentSharedStateTracker.getMaxPacketAmountTracker()
          .getNextMaxPacketAmount();
        UnsignedLong sourceAmount = FluentUnsignedLong
          .of(FluentBigInteger.of(availableToSend).orMaxUnsignedLong()).orLesser(maxPacketAmount).getValue();

        // Check if fixed delivery payment is complete, and apply limits
        if (target.paymentType() == PaymentType.FIXED_DELIVERY) {
          final boolean paidFixedDelivery = this.checkIfFixedDeliveryPaymentIsComplete(amountTracker, target);
          if (paidFixedDelivery) {
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("Payment complete: paid fixed destination amount. {} of {}",
                amountTracker.getAmountDeliveredInDestinationUnits(), target.minDeliveryAmount()
              );
            }
            streamPacketRequest.setStreamErrorCodeForConnectionClose(ErrorCodes.NoError);
            return SendState.End;
          }

          if (this.moreAvailableToDeliver(amountTracker, target) == NO_MORE_TO_DELIVER) {
            return SendState.Wait; // TODO: Should this be ready, like JS, or wait instead?
          }

          if (this.isSourceAmountDeliveryLimitInvalid(amountTracker, target)) {
            LOGGER.warn("Payment cannot complete: exchange rate dropped to 0");
            streamPacketRequest.setStreamErrorCodeForConnectionClose(ErrorCodes.NoError);
            return SendState.InsufficientExchangeRate;
          }

          final UnsignedLong sourceAmountDeliveryLimit = this.computeSourceAmountDeliveryLimit(amountTracker, target);
          sourceAmount = FluentUnsignedLong.of(sourceAmount).orLesser(sourceAmountDeliveryLimit).getValue();
        }

        // Enforce the minimum exchange rate, and estimate how much will be received
        UnsignedLong minDestinationAmount = this.computeMinDestinationAmount(
          streamPacketRequest.sourceAmount(), target.minExchangeRate()
        );
        final UnsignedLong estimatedDestinationAmount = computeEstimatedDestinationAmount(sourceAmount);

        // Only allow a destination shortfall within the allowed margins *on the final packet*.
        // If the packet is insufficient to complete the payment, the rate dropped and cannot be completed.
        final UnsignedLong deliveryDeficit = computeDeliveryDeficit(minDestinationAmount, estimatedDestinationAmount);
        if (FluentUnsignedLong.of(deliveryDeficit).isPositive()) {
          // Is it probable that this packet will complete the payment?
          boolean willPaymentComplete =
            this.willPaymentComplete(amountTracker, target, availableToSend, sourceAmount, estimatedDestinationAmount);

          if (!willPaymentComplete ||
            FluentCompareTo.is(amountTracker.getAvailableDeliveryShortfall())
              .lessThan(deliveryDeficit.bigIntegerValue())
          ) {
            if (LOGGER.isWarnEnabled()) {
              LOGGER.warn("Payment cannot complete: exchange rate dropped below minimum");
            }
            streamPacketRequest.setStreamErrorCodeForConnectionClose(ErrorCodes.NoError);
            return SendState.InsufficientExchangeRate;
          }

          minDestinationAmount = estimatedDestinationAmount;
        }

        streamPacketRequest.setSourceAmount(sourceAmount);
        streamPacketRequest.setMinDestinationAmount(minDestinationAmount);
        streamPacketRequest.setRequestFrames(Lists.newArrayList(
          StreamMoneyFrame.builder()
            .streamId(UnsignedLong.ONE) // TODO: Use default streamId.
            .shares(UnsignedLong.ONE)
            .build())
        );

        return SendState.Ready; // <-- Send the packet.
      })
      // No fixed source or delivery amount set
      .orElse(SendState.Ready);
  }

  @Override
  public StreamPacketReply doFilter(
    final StreamPacketRequest streamPacketRequest, final StreamPacketFilterChain filterChain
  ) {
    Objects.requireNonNull(streamPacketRequest);
    Objects.requireNonNull(filterChain);

    final AmountTracker amountTracker = paymentSharedStateTracker.getAmountTracker();
    UnsignedLong highEndDestinationAmount = UnsignedLong.ZERO;
    UnsignedLong deliveryDeficit = UnsignedLong.ZERO;

    // Update in-flight amounts
    amountTracker.addToSourceAmountInFlight(streamPacketRequest.sourceAmount());
    amountTracker.addToDestinationAmountInFlight(highEndDestinationAmount);

    if (amountTracker.getPaymentTargetConditions().isPresent()) {
      PaymentTargetConditions target = amountTracker.getPaymentTargetConditions().get();

      // Estimate the most that this packet will deliver
      highEndDestinationAmount = FluentUnsignedLong.of(streamPacketRequest.minDestinationAmount())
        .orGreater(
          paymentSharedStateTracker.getExchangeRateTracker()
            .estimateDestinationAmount(streamPacketRequest.sourceAmount()).highEndEstimate()
        ).getValue();

      // Update the delivery shortfall, if applicable
      final UnsignedLong baselineMinDestinationAmount =
        this.computeMinDestinationAmount(streamPacketRequest.sourceAmount(), target.minExchangeRate());
      deliveryDeficit = baselineMinDestinationAmount.minus(streamPacketRequest.minDestinationAmount());
      if (FluentUnsignedLong.of(deliveryDeficit).isPositive()) {
        amountTracker.reduceDeliveryShortfall(deliveryDeficit);
      }
    }

    final StreamPacketReply streamPacketReply = filterChain.doFilter(streamPacketRequest);

    Optional<UnsignedLong> destinationAmount = streamPacketReply.destinationAmountClaimed();

    if (streamPacketReply.isFulfill()) {
      // Delivered amount must be *at least* the minimum acceptable amount we told the receiver
      // No matter what, since they fulfilled it, we must assume they got at least the minimum
      if (!streamPacketReply.destinationAmountClaimed().isPresent()) {
        // Technically, an intermediary could strip the data so we can't ascertain whose fault this is
        LOGGER.warn("Ending payment: packet fulfilled with no authentic STREAM data");
        destinationAmount = Optional.of(streamPacketRequest.minDestinationAmount());
        amountTracker.setEncounteredProtocolViolation();
      } else if (destinationAmount.isPresent() && FluentCompareTo.is(destinationAmount.get())
        .lessThan(streamPacketRequest.minDestinationAmount())) {
        if (LOGGER.isWarnEnabled()) {
          LOGGER.warn(
            "Ending payment: receiver violated protocol. packet below minimum exchange rate was fulfilled. "
              + "destinationAmount={}  minDestinationAmount={}",
            destinationAmount,
            streamPacketRequest.minDestinationAmount()
          );
        }
        destinationAmount = Optional.of(streamPacketRequest.minDestinationAmount());
        amountTracker.setEncounteredProtocolViolation();
      } else {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace(
            "Packet sent: sourceAmount={}  destinationAmount={} minDestinationAmount={}",
            streamPacketRequest.sourceAmount(), destinationAmount, streamPacketRequest.minDestinationAmount()
          );
        }
      }

      amountTracker.addAmountSent(streamPacketRequest.sourceAmount());
      amountTracker.addAmountDelivered(destinationAmount);
    } else if (destinationAmount.isPresent() && FluentCompareTo.is(destinationAmount.get())
      .lessThan(streamPacketRequest.minDestinationAmount())) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
          "Packet rejected for insufficient rate: minDestinationAmount={} destinationAmount={}",
          streamPacketRequest.minDestinationAmount(), destinationAmount
        );
      }
    }

    amountTracker.subtractFromSourceAmountInFlight(streamPacketRequest.sourceAmount());
    amountTracker.subtractFromDestinationAmountInFlight(highEndDestinationAmount);

    // If this packet failed, "refund" the delivery deficit so it may be retried
    if (FluentUnsignedLong.of(deliveryDeficit).isPositive() && streamPacketReply.isReject()) {
      amountTracker.increaseDeliveryShortfall(deliveryDeficit);
    }

    amountTracker.getPaymentTargetConditions().ifPresent(target -> {
      if (target.paymentType() == PaymentType.FIXED_SEND) {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace(
            "Fixed Send Payment has sent {} of {}, {} in-flight",
            amountTracker.getAmountSentInSourceUnits(),
            target.maxSourceAmount(),
            amountTracker.getSourceAmountInFlight()
          );
        }
      } else if (target.paymentType() == PaymentType.FIXED_DELIVERY) {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace(
            "Fixed Delivery Payment has sent {} of {}, {} in-flight",
            amountTracker.getAmountDeliveredInDestinationUnits(),
            target.minDeliveryAmount(),
            amountTracker.getDestinationAmountInFlight()
          );
        }
      }
    });

    this.updateReceiveMax(streamPacketReply);

    return streamPacketReply;
  }

  @VisibleForTesting
  protected void updateReceiveMax(final StreamPacketReply streamPacketReply) {
    Objects.requireNonNull(streamPacketReply);

    StreamPacketUtils.findStreamMaxMoneyFrames(streamPacketReply.frames()).stream()
      .filter(streamMoneyMaxFrame -> streamMoneyMaxFrame.streamId().equals(StreamPacketUtils.DEFAULT_STREAM_ID))
      .forEach(streamMoneyMaxFrame -> {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace(
            "Recipient told us the stream has received {} of up to {}",
            streamMoneyMaxFrame.totalReceived(), streamMoneyMaxFrame.receiveMax()
          );
        }

        // Note: totalReceived *can* be greater than receiveMax! (`ilp-protocol-stream` allows receiving 1% more than the receiveMax)
        UnsignedLong receiveMax = streamMoneyMaxFrame.receiveMax();
        // Remote receive max can only increase
        paymentSharedStateTracker.getAmountTracker().updateRemoteMax(FluentUnsignedLong
          .of(paymentSharedStateTracker.getAmountTracker().getRemoteReceivedMax().orElse(UnsignedLong.ZERO))
          .orGreater(receiveMax).getValue()
        );
      });
  }

  /**
   * Checks the {@code streamPacketRequest} to determine if the receiver's "receiveMax" is large enough to accomadate
   * the minimum delivery amount required by the sender.
   *
   * @param amountTracker An {@link AmountTracker}.
   * @param target        A {@link PaymentTargetConditions}.
   *
   * @return {@code true} if the remote's receive-max is not compatible with the sender's minimum amount; otherwise
   *   {@code false}.
   */
  @VisibleForTesting
  protected boolean checkForIncompatibleReceiveMax(
    final AmountTracker amountTracker, final PaymentTargetConditions target
  ) {
    Objects.requireNonNull(amountTracker);
    Objects.requireNonNull(target);

    // Is the recipient's advertised `receiveMax` less than the fixed destination amount?
    final boolean incompatibleReceiveMax = amountTracker.getRemoteReceivedMax()
      .filter(remoteReceivedMax ->
        FluentCompareTo.is(target.minDeliveryAmount()).greaterThan(remoteReceivedMax.bigIntegerValue())
      ).isPresent();

    return incompatibleReceiveMax;
  }

  /**
   * When a payment is of type {@link PaymentType#FIXED_SEND}, this method checks to see if the payment has been fully
   * paid.
   *
   * @param amountTracker An {@link AmountTracker}.
   * @param target        A {@link PaymentTargetConditions}.
   *
   * @return {@code true} if a FIXED_SEND payment has been completely paid; {@code false} otherwise.
   */
  @VisibleForTesting
  protected boolean checkIfFixedSendPaymentIsComplete(
    final AmountTracker amountTracker, final PaymentTargetConditions target
  ) {
    Objects.requireNonNull(amountTracker);
    Objects.requireNonNull(target);

    if (target.paymentType() == PaymentType.FIXED_SEND) {
      final boolean paidFixedSend =
        FluentCompareTo.is(amountTracker.getAmountSentInSourceUnits()).equalTo(target.maxSourceAmount()) &&
          FluentBigInteger.of(amountTracker.getSourceAmountInFlight()).isNotPositive();
      return paidFixedSend;
    } else {
      return false;
    }
  }

  /**
   * When a payment is of type {@link PaymentType#FIXED_DELIVERY}, this method checks to see if the payment has been
   * fully paid.
   *
   * @param amountTracker An {@link AmountTracker}.
   * @param target        A {@link PaymentTargetConditions}.
   *
   * @return {@code true} if a FIXED_DELIVERY payment has been completely paid; {@code false} otherwise.
   */
  @VisibleForTesting
  protected boolean checkIfFixedDeliveryPaymentIsComplete(
    final AmountTracker amountTracker, final PaymentTargetConditions target
  ) {
    Objects.requireNonNull(amountTracker);
    Objects.requireNonNull(target);

    final BigInteger remainingToDeliver = this.computeRemainingAmountToBeDelivered(amountTracker, target);
    final boolean paidFixedDelivery =
      FluentCompareTo.is(remainingToDeliver).lessThanOrEqualTo(BigInteger.ZERO) && // <-- Everything delivered
        FluentBigInteger.of(amountTracker.getSourceAmountInFlight()).isNotPositive(); // <-- No more can be in-flight
    return paidFixedDelivery;
  }

  /**
   * Compute the remaining amount to deliver, in destination units. Note that this value does not include "in-flight"
   * amounts because those have not yet technically been "delivered."
   *
   * @param amountTracker An {@link AmountTracker}.
   * @param target        A {@link PaymentTargetConditions}.
   *
   * @return A {@link BigInteger} representing the total amount remaining to be delivered.
   */
  @VisibleForTesting
  protected BigInteger computeRemainingAmountToBeDelivered(
    final AmountTracker amountTracker, final PaymentTargetConditions target
  ) {
    Objects.requireNonNull(amountTracker);
    Objects.requireNonNull(target);

    return target.minDeliveryAmount().subtract(amountTracker.getAmountDeliveredInDestinationUnits());
  }

  /**
   * Determine if more packets can be delivered. Note that this calculation does include "in-flight" amounts because
   * those may complete, and if they do, we don't want to duplicate them.
   *
   * @param amountTracker
   * @param target
   *
   * @return
   */
  @VisibleForTesting
  protected boolean moreAvailableToDeliver(
    final AmountTracker amountTracker, final PaymentTargetConditions target
  ) {
    Objects.requireNonNull(amountTracker);
    Objects.requireNonNull(target);

    final BigInteger amountAvailableToDeliver = this.computeAmountAvailableToDeliver(amountTracker, target);
    return FluentBigInteger.of(amountAvailableToDeliver).isPositive();
  }

  /**
   * Compute the total number of units left to send (in sender's units).
   *
   * @param target A {@link PaymentTargetConditions}.
   *
   * @return A {@link BigInteger} representing the total amout to send.
   */
  @VisibleForTesting
  protected BigInteger computeAmountAvailableToSend(final PaymentTargetConditions target) {
    Objects.requireNonNull(target);

    return target.maxSourceAmount()
      .subtract(paymentSharedStateTracker.getAmountTracker().getAmountSentInSourceUnits())
      .subtract(paymentSharedStateTracker.getAmountTracker().getSourceAmountInFlight());
  }

  /**
   * Compute the total number of units left to send (in sender's units).
   *
   * @param target A {@link PaymentTargetConditions}.
   *
   * @return A {@link BigInteger} representing the total amout to send.
   */
  @VisibleForTesting
  protected BigInteger computeAmountAvailableToDeliver(
    final AmountTracker amountTracker, final PaymentTargetConditions target
  ) {
    Objects.requireNonNull(amountTracker);
    Objects.requireNonNull(target);

    return this.computeRemainingAmountToBeDelivered(amountTracker, target)
      .subtract(amountTracker.getDestinationAmountInFlight());
  }

  @VisibleForTesting
  protected UnsignedLong computeSourceAmountDeliveryLimit(
    final AmountTracker amountTracker, final PaymentTargetConditions target
  ) {
    Objects.requireNonNull(amountTracker);
    Objects.requireNonNull(target);

    final BigInteger availableToDeliver = this.computeAmountAvailableToDeliver(amountTracker, target);
    final UnsignedLong availableToDeliverUL = FluentBigInteger.of(availableToDeliver).orMaxUnsignedLong();

    return this.paymentSharedStateTracker.getExchangeRateTracker()
      .estimateSourceAmount(availableToDeliverUL)
      .highEndEstimate();
  }

  @VisibleForTesting
  protected boolean isSourceAmountDeliveryLimitInvalid(
    final AmountTracker amountTracker, final PaymentTargetConditions target
  ) {
    Objects.requireNonNull(amountTracker);
    Objects.requireNonNull(target);

    final UnsignedLong sourceAmountDeliveryLimit = this.computeSourceAmountDeliveryLimit(amountTracker, target);
    return FluentCompareTo.is(sourceAmountDeliveryLimit).lessThanOrEqualTo(UnsignedLong.ZERO);
  }

  /**
   * Compute the estimated destination amount based upon the supplied {@code sourceAmount} and an implied exchange rate
   * taken from the {@link ExchangeRateTracker}.
   *
   * @param sourceAmount An {@link UnsignedLong} representing the original source amount of a payment, in sender's
   *                     units.
   *
   * @return An {@link UnsignedLong} representing the minimum destination amount.
   */
  @VisibleForTesting
  protected UnsignedLong computeEstimatedDestinationAmount(final UnsignedLong sourceAmount) {
    Objects.requireNonNull(sourceAmount);
    return paymentSharedStateTracker.getExchangeRateTracker().estimateDestinationAmount(sourceAmount).lowEndEstimate();
  }

  /**
   * Compute the minimum destination amount based upon the supplied {@code sourceAmount} and {@code minExchangeRate}.
   *
   * @param sourceAmount    An {@link UnsignedLong} representing the original source amount of a payment, in sender's
   *                        units.
   * @param minExchangeRate An {@link UnsignedLong} representing the minimum acceptable exchange rate, as discovered by
   *                        the path and overall payment input parameters.
   *
   * @return An {@link UnsignedLong} representing the minimum destination amount.
   */
  @VisibleForTesting
  protected UnsignedLong computeMinDestinationAmount(
    final UnsignedLong sourceAmount, final BigDecimal minExchangeRate
  ) {
    Objects.requireNonNull(sourceAmount);
    Objects.requireNonNull(minExchangeRate);
    return FluentUnsignedLong.of(sourceAmount).timesCeil(Ratio.from(minExchangeRate)).getValue();
  }

  /**
   * Compute the delivery deficit, which is the delta between what we expect will be delivered, and the minimum
   * destination amount. If this number is positive (i.e., greater-than-zero), it indicates that we likely won't be able
   * to deliver the full amount of a payment, because we need the estimate, but it's less than the minimum acceptable
   * amount, which means nothing will get delivered.
   *
   * @param minDestinationAmount       An {@link UnsignedLong} representing the minimum acceptable destination amount,
   *                                   in destination units.
   * @param estimatedDestinationAmount An {@link UnsignedLong} representing the estimated amount that will be delivered,
   *                                   in destination units.
   *
   * @return An {@link UnsignedLong} representing the anticipated delivery deficit, if any. Note that this operation is
   *   dehydrating, so any negative number will be returned as {@link UnsignedLong#ZERO}.
   */
  @VisibleForTesting
  protected UnsignedLong computeDeliveryDeficit(
    final UnsignedLong minDestinationAmount, final UnsignedLong estimatedDestinationAmount
  ) {
    Objects.requireNonNull(minDestinationAmount);
    Objects.requireNonNull(estimatedDestinationAmount);

    return FluentUnsignedLong.of(minDestinationAmount).minusOrZero(estimatedDestinationAmount).getValue();
  }

  /**
   * Determines if the payment will complete.
   *
   * @param amountTracker              A {@link AmountTracker}.
   * @param target                     A {@link PaymentTargetConditions}.
   * @param availableToSend            A {@link BigInteger} representing the amount available to be sent (in sender
   *                                   units).
   * @param sourceAmount               A {@link BigInteger} representing the overall source amount to be sent (in sender
   *                                   units).
   * @param estimatedDestinationAmount A {@link UnsignedLong} representing the estimated amount for the receiver, in
   *                                   destination units.
   *
   * @return {@code true} if the payment will complete; {@code false} if it will not.
   */
  @VisibleForTesting
  protected boolean willPaymentComplete(
    final AmountTracker amountTracker, final PaymentTargetConditions target,
    final BigInteger availableToSend,
    final UnsignedLong sourceAmount,
    final UnsignedLong estimatedDestinationAmount
  ) {
    return target.paymentType() == PaymentType.FIXED_SEND
      ? sourceAmount.bigIntegerValue().equals(availableToSend)
      : FluentCompareTo.is(
        amountTracker.getAmountDeliveredInDestinationUnits()
          .add(amountTracker.getDestinationAmountInFlight())
          .add(estimatedDestinationAmount.bigIntegerValue())
      ).greaterThanEqualTo(target.minDeliveryAmount());
  }

}
