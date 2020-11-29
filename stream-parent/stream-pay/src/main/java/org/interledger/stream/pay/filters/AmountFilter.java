package org.interledger.stream.pay.filters;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedLong;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;
import org.interledger.core.fluent.FluentBigInteger;
import org.interledger.core.fluent.FluentCompareTo;
import org.interledger.core.fluent.FluentUnsignedLong;
import org.interledger.core.fluent.Ratio;
import org.interledger.stream.StreamPacketUtils;
import org.interledger.stream.frames.ErrorCodes;
import org.interledger.stream.frames.StreamMoneyFrame;
import org.interledger.stream.pay.filters.chain.StreamPacketFilterChain;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.model.StreamPacketReply;
import org.interledger.stream.pay.model.StreamPacketRequest;
import org.interledger.stream.pay.probing.model.PaymentTargetConditions;
import org.interledger.stream.pay.probing.model.PaymentTargetConditions.PaymentType;
import org.interledger.stream.pay.trackers.AmountTracker;
import org.interledger.stream.pay.trackers.PaymentSharedStateTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.interledger.stream.pay.model.ModifiableStreamPacketRequest;

/**
 * Tracks and calculates amounts to send and deliver.
 */
public class AmountFilter implements StreamPacketFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(AmountFilter.class);

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
        // Is the recipient's advertised `receiveMax` less than the fixed destination amount?
        // TODO: This implies that the total payment received on a stream cannot exceed UnsignedLong.MAX_VALUE. If this
        // is correct, then there is no need to use BigInteger. However, if no max is sent, then it's possible that
        // a BigInteger might be preferable. Create a test to validate this with a simulated link?
        final boolean incompatibleReceiveMax = amountTracker.getRemoteReceivedMax()
          .filter(remoteReceivedMax -> FluentCompareTo.is(target.minDeliveryAmount())
            .greaterThan(remoteReceivedMax.bigIntegerValue()))
          .isPresent();

        if (incompatibleReceiveMax) {
          LOGGER.error(String.format(
            "Ending payment: minimum delivery amount is too much for recipient. "
              + "minDeliveryAmount={} remoteReceiveMax={}",
            target.minDeliveryAmount(), amountTracker.getRemoteReceivedMax()
          ));
          streamPacketRequest.setStreamErrorCodeForConnectionClose(ErrorCodes.ApplicationError);
          return SendState.IncompatibleReceiveMax;
        }

        if (target.paymentType() == PaymentType.FIXED_SEND) {
          final boolean paidFixedSend =
            FluentCompareTo.is(amountTracker.getAmountSentInSourceUnits()).equalTo(target.maxSourceAmount()) &&
              FluentBigInteger.of(amountTracker.getSourceAmountInFlight()).isPositive();
          if (paidFixedSend) {
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug(
                "Payment complete: paid fixed source amount. sent {}",
                amountTracker.getAmountSentInSourceUnits()
              );
            }
            streamPacketRequest.setStreamErrorCodeForConnectionClose(ErrorCodes.NoError);
            return SendState.End;
          }
        }

        // Ensure we never overpay the maximum source amount. Note that this only works because this evaluation is
        // performed as part of the `nextState` operations, which are always run on a single-thread in the RunLoop.
        // Thus, this calculation is always accurate (i.e., there is no other thread that could be mutating the
        // available send to be higher than it should be because multi-threaded behavior is only ever reducing the
        // AmountSentInSourceUnits, never increasing it.
        final BigInteger availableToSend = target.maxSourceAmount()
          .subtract(amountTracker.getAmountSentInSourceUnits())
          .subtract(amountTracker.getSourceAmountInFlight());
        if (FluentBigInteger.of(availableToSend).isNotPositive()) {
          return SendState.Wait; // <-- Don't schedule this packet, but keep trying in the run-loop.
        }

        // Compute source amount (always positive)
        final UnsignedLong maxPacketAmount = paymentSharedStateTracker.getMaxPacketAmountTracker()
          .getNextMaxPacketAmount();
        UnsignedLong sourceAmount = FluentUnsignedLong.of(FluentBigInteger.of(availableToSend).orMaxUnsignedLong())
          .orLesser(maxPacketAmount).getValue();

        // Check if fixed delivery payment is complete, and apply limits
        if (target.paymentType() == PaymentType.FIXED_DELIVERY) {
          // TODO: Unit Test Fixed Delivery once supported!
          final BigInteger remainingToDeliver = target.minDeliveryAmount()
            .subtract(amountTracker.getAmountDeliveredInDestinationUnits());
          final boolean paidFixedDelivery =
            FluentCompareTo.is(remainingToDeliver).lessThanOrEqualTo(BigInteger.ZERO) &&
              FluentBigInteger.of(amountTracker.getSourceAmountInFlight()).isPositive();
          if (paidFixedDelivery) {
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("Payment complete: paid fixed destination amount. {} of {}",
                amountTracker.getAmountDeliveredInDestinationUnits(), target.minDeliveryAmount()
              );
            }
            streamPacketRequest.setStreamErrorCodeForConnectionClose(ErrorCodes.NoError);
            return SendState.End;
          }

          final BigInteger availableToDeliver = remainingToDeliver
            .subtract(amountTracker.getDestinationAmountInFlight());
          if (FluentBigInteger.of(availableToDeliver).isNotPositive()) {
            return SendState.Ready;
          }

          final UnsignedLong availableToDeliverUL = FluentCompareTo.is(availableToDeliver)
            .greaterThan(UnsignedLong.MAX_VALUE.bigIntegerValue()) ? UnsignedLong.MAX_VALUE :
            UnsignedLong.valueOf(availableToDeliver);

          final UnsignedLong sourceAmountDeliveryLimit = paymentSharedStateTracker.getExchangeRateTracker()
            .estimateSourceAmount(availableToDeliverUL).highEndEstimate();
          if (FluentCompareTo.is(sourceAmountDeliveryLimit).lessThanOrEqualTo(UnsignedLong.ZERO)) {
            LOGGER.warn("Payment cannot complete: exchange rate dropped to 0");
            streamPacketRequest.setStreamErrorCodeForConnectionClose(ErrorCodes.NoError);
            return SendState.InsufficientExchangeRate;
          }

          sourceAmount = FluentUnsignedLong.of(sourceAmount).orLesser(sourceAmountDeliveryLimit).getValue();
        }

        // Enforce the minimum exchange rate, and estimate how much will be received
        UnsignedLong minDestinationAmount = FluentUnsignedLong.of(sourceAmount)
          .timesCeil(Ratio.from(target.minExchangeRate())).getValue();
        final UnsignedLong estimatedDestinationAmount = paymentSharedStateTracker.getExchangeRateTracker()
          .estimateDestinationAmount(sourceAmount).lowEndEstimate();

        // Only allow a destination shortfall within the allowed margins *on the final packet*.
        // If the packet is insufficient to complete the payment, the rate dropped and cannot be completed.
        final UnsignedLong deliveryDeficit = FluentUnsignedLong.of(minDestinationAmount)
          .minusOrZero(estimatedDestinationAmount).getValue();
        if (FluentUnsignedLong.of(deliveryDeficit).isPositive()) {
          // Is it probable that this packet will complete the payment?
          boolean completesPayment =
            target.paymentType() == PaymentType.FIXED_SEND
              ? sourceAmount.bigIntegerValue().equals(availableToSend)
              : FluentCompareTo.is(
                amountTracker.getAmountDeliveredInDestinationUnits()
                  .add(amountTracker.getDestinationAmountInFlight())
                  .add(estimatedDestinationAmount.bigIntegerValue())
              ).greaterThanEqualTo(target.minDeliveryAmount());

          if (
            FluentCompareTo.is(amountTracker.getAvailableDeliveryShortfall())
              .lessThan(deliveryDeficit.bigIntegerValue()) || !completesPayment
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

    if (amountTracker.getPaymentTargetConditions().isPresent()) {
      PaymentTargetConditions target = amountTracker.getPaymentTargetConditions().get();

      // Estimate the most that this packet will deliver
      highEndDestinationAmount = FluentUnsignedLong.of(streamPacketRequest.minDestinationAmount())
        .orGreater(
          paymentSharedStateTracker.getExchangeRateTracker()
            .estimateDestinationAmount(streamPacketRequest.sourceAmount()).highEndEstimate()
        ).getValue();

      // Update in-flight amounts
      amountTracker.addToSourceAmountInFlight(streamPacketRequest.sourceAmount());
      amountTracker.addToDestinationAmountInFlight(highEndDestinationAmount);

      // Update the delivery shortfall, if applicable
      final UnsignedLong baselineMinDestinationAmount = FluentUnsignedLong.of(streamPacketRequest.sourceAmount())
        .timesCeil(Ratio.from(target.minExchangeRate())).getValue();
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

    amountTracker.subtractFromDestinationAmountInFlight(streamPacketRequest.sourceAmount());
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
}
