package org.interledger.stream.pay.filters;

import static org.interledger.stream.StreamPacketUtils.DEFAULT_STREAM_ID;

import org.interledger.core.fluent.FluentBigInteger;
import org.interledger.core.fluent.FluentCompareTo;
import org.interledger.core.fluent.FluentUnsignedLong;
import org.interledger.core.fluent.Ratio;
import org.interledger.stream.StreamPacketUtils;
import org.interledger.stream.frames.StreamMoneyFrame;
import org.interledger.stream.pay.exceptions.StreamPayerException;
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

  // Static because this filter will be constructed a lot.
  private static final Logger LOGGER = LoggerFactory.getLogger(AmountFilter.class);

  private static final boolean NO_MORE_TO_DELIVER = false;
  private static final boolean NOT_VALID = false;

  private final PaymentSharedStateTracker paymentSharedStateTracker;

  public AmountFilter(final PaymentSharedStateTracker paymentSharedStateTracker) {
    this.paymentSharedStateTracker = Objects.requireNonNull(paymentSharedStateTracker);
  }

  @Override
  public synchronized SendState nextState(final ModifiableStreamPacketRequest streamPacketRequest) {
    Objects.requireNonNull(streamPacketRequest);

    final AmountTracker amountTracker = paymentSharedStateTracker.getAmountTracker();

    if (amountTracker.encounteredProtocolViolation()) {
      throw new StreamPayerException(
        "Stream Protocol violation encountered.", SendState.ReceiverProtocolViolation
      );
    }

    // If there are no PaymentTargetConditions, then always return READY (ergo, this `map` is correct).
    return amountTracker.getPaymentTargetConditions()
      .map(target -> {

        // Check ReceiveMax vs SendMin.
        if (this.checkForIncompatibleReceiveMax(amountTracker, target)) {
          final String errorMessage = String.format(
            "Ending payment: minimum delivery amount is too much for recipient. "
              + "minDeliveryAmount=%s remoteReceiveMax=%s",
            target.minPaymentAmountInDestinationUnits(),
            amountTracker.getRemoteReceivedMax()
          );
          throw new StreamPayerException(errorMessage, SendState.IncompatibleReceiveMax);
        }

        // Check PaidFixedSend
        if (this.checkIfFixedSendPaymentIsComplete(amountTracker, target)) {
          LOGGER.info(
            "Payment complete: paid fixed source amount. sent {}",
            amountTracker.getAmountSentInSourceUnits()
          );
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

        if (!anyAvailableToSend) { // <-- anyAvailableToSend is false if we get here.
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
        UnsignedLong sourcePacketAmount = FluentUnsignedLong
          .of(FluentBigInteger.of(availableToSend).orMaxUnsignedLong()).orLesser(maxPacketAmount).getValue();

        // Check if fixed delivery payment is complete, and apply limits
        if (target.paymentType() == PaymentType.FIXED_DELIVERY) {
          final boolean paidFixedDelivery = this.checkIfFixedDeliveryPaymentIsComplete(amountTracker, target);
          if (paidFixedDelivery) {
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("Payment complete: paid fixed destination amount. {} of {}",
                amountTracker.getAmountDeliveredInDestinationUnits(), target.minPaymentAmountInDestinationUnits()
              );
            }
            return SendState.End;
          }

          if (this.moreAvailableToDeliver(amountTracker, target) == NO_MORE_TO_DELIVER) {
            return SendState.Ready;
          }

          if (this.isSourceAmountDeliveryLimitInvalid(amountTracker, target)) {
            throw new StreamPayerException(
              "Payment cannot complete: exchange rate dropped to 0", SendState.InsufficientExchangeRate
            );
          }

          final UnsignedLong sourcePacketAmountDeliveryLimit = this
            .computeSourceAmountDeliveryLimit(amountTracker, target);
          sourcePacketAmount = FluentUnsignedLong.of(sourcePacketAmount).orLesser(sourcePacketAmountDeliveryLimit)
            .getValue();
        }

        // Enforce the minimum exchange rate, and estimate how much will be received.
        UnsignedLong minDestinationPacketAmount = this.computeMinDestinationPacketAmount(
          sourcePacketAmount, target.minExchangeRate()
        );
        final UnsignedLong estimatedDestinationPacketAmount = computeEstimatedDestinationAmount(sourcePacketAmount);

        // Only allow a destination shortfall within the allowed margins *on the final packet*.
        // If the packet is insufficient to complete the payment, the rate dropped and cannot be completed.
        final UnsignedLong packetDeliveryDeficit = computePacketDeliveryDeficitForNextState(
          minDestinationPacketAmount, estimatedDestinationPacketAmount
        );
        if (FluentUnsignedLong.of(packetDeliveryDeficit).isPositive()) {
          // Is it probable that this packet will complete the payment?
          boolean willPaymentComplete = this.willPaymentComplete(
            amountTracker, target, availableToSend, sourcePacketAmount, estimatedDestinationPacketAmount
          );

          if (!willPaymentComplete ||
            FluentCompareTo.is(amountTracker.getAvailableDeliveryShortfall()).lessThan(packetDeliveryDeficit)
          ) {
            String errorMessage = String
              .format("Payment cannot complete because exchange rate dropped below minimum. " +
                  "availableToSend=%s "
                  + "sourcePacketAmount=%s "
                  + "estimatedDestinationPacketAmount=%s "
                  + "availableDeliverShortfall=%s "
                  + "packetDeliverDeficiti=%s",
                availableToSend,
                sourcePacketAmount,
                estimatedDestinationPacketAmount,
                amountTracker.getAvailableDeliveryShortfall(),
                packetDeliveryDeficit
              );
            throw new StreamPayerException(errorMessage, SendState.InsufficientExchangeRate);
          }

          minDestinationPacketAmount = estimatedDestinationPacketAmount;
        }

        streamPacketRequest.setSourceAmount(sourcePacketAmount);
        streamPacketRequest.setMinDestinationAmount(minDestinationPacketAmount);
        streamPacketRequest.setRequestFrames(Lists.newArrayList(
          StreamMoneyFrame.builder()
            .streamId(DEFAULT_STREAM_ID)
            .shares(UnsignedLong.ONE)
            .build())
        );

        // Reserve the packet here. It's OK to do this at the end of this computation because only a single thread
        // operates in `nextState` at a time, so reserving here is fine.
        amountTracker.addToSourceAmountScheduled(streamPacketRequest.sourceAmount());

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
    UnsignedLong packetDeliveryDeficit = UnsignedLong.ZERO;

    // Update in-flight amounts
    amountTracker.addToSourceAmountInFlight(streamPacketRequest.sourceAmount());
    amountTracker.addToDestinationAmountInFlight(highEndDestinationAmount);
    try {
      if (amountTracker.getPaymentTargetConditions().isPresent()) {
        PaymentTargetConditions target = amountTracker.getPaymentTargetConditions().get();

        // Estimate the most that this packet will deliver
        highEndDestinationAmount = FluentUnsignedLong.of(streamPacketRequest.minDestinationAmount())
          .orGreater(
            paymentSharedStateTracker.getExchangeRateTracker()
              .estimateDestinationAmount(streamPacketRequest.sourceAmount()).highEndEstimate()
          ).getValue();

        // Update the delivery shortfall, if applicable
        packetDeliveryDeficit = this.computePacketDeliveryDeficitForDoFilter(
          streamPacketRequest.sourceAmount(), target.minExchangeRate(), streamPacketRequest.minDestinationAmount()
        );
        if (FluentUnsignedLong.of(packetDeliveryDeficit).isPositive()) {
          amountTracker.reduceDeliveryShortfall(packetDeliveryDeficit);
        }
      }

      final StreamPacketReply streamPacketReply = filterChain.doFilter(streamPacketRequest);

      streamPacketReply.handle(
        fulfilledStreamPacketReply -> {
          final UnsignedLong destinationAmount = streamPacketReply.destinationAmountClaimed()
            // Delivered amount must be *at least* the minimum acceptable amount we told the receiver
            // No matter what, since they fulfilled it, we must assume they got at least the minimum
            .map(destinationAmountClaimed -> {
              if (
                this.isDestinationAmountValid(destinationAmountClaimed,
                  streamPacketRequest.minDestinationAmount()) == NOT_VALID
              ) {
                LOGGER.error(
                  "Ending payment: Receiver violated protocol (packet below minimum exchange rate was fulfilled). " +
                    "destinationAmountClaimed={}  minDestinationAmount={}",
                  destinationAmountClaimed, streamPacketRequest.minDestinationAmount()
                );
                amountTracker.setEncounteredProtocolViolation();
                return streamPacketRequest.minDestinationAmount();
              } else {
                if (LOGGER.isTraceEnabled()) {
                  LOGGER.trace(
                    "Packet Sent and Fulfilled: sourceAmount={} destinationAmountClaimed={} minDestinationAmount={}",
                    streamPacketRequest.sourceAmount(), destinationAmountClaimed,
                    streamPacketRequest.minDestinationAmount()
                  );
                }
                return destinationAmountClaimed;
              }
            })
            .orElseGet(() -> {
              // Technically, an intermediary could strip the data so we can't ascertain whose fault this is
              LOGGER.error("Ending payment: packet fulfilled with no authentic STREAM data");
              amountTracker.setEncounteredProtocolViolation();
              return streamPacketRequest.minDestinationAmount();
            });

          amountTracker.addAmountSent(streamPacketRequest.sourceAmount());
          amountTracker.addAmountDelivered(destinationAmount);
        },
        rejectedStreamPacketReply -> {
          final boolean destinationAmountValid = isDestinationAmountValid(
            streamPacketReply.destinationAmountClaimed(), streamPacketRequest.minDestinationAmount()
          );
          if (destinationAmountValid == NOT_VALID) {
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug(
                "Packet rejected for insufficient rate: minDestinationAmount={} claimedDestinationAmount={}",
                streamPacketRequest.minDestinationAmount(), streamPacketReply.destinationAmountClaimed()
              );
            }
          }
        });

      // If this packet failed, "refund" the delivery deficit so it may be retried.
      if (this.isFailedPacket(packetDeliveryDeficit, streamPacketReply)) {
        amountTracker.increaseDeliveryShortfall(packetDeliveryDeficit);
      }

      amountTracker.getPaymentTargetConditions().ifPresent(target -> {
        if (target.paymentType() == PaymentType.FIXED_SEND) {
          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(
              "Fixed Send Payment has sent {} of {}, {} in-flight",
              amountTracker.getAmountSentInSourceUnits(),
              target.maxPaymentAmountInSenderUnits(),
              amountTracker.getSourceAmountInFlight()
            );
          }
        } else if (target.paymentType() == PaymentType.FIXED_DELIVERY) {
          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(
              "Fixed Delivery Payment has sent {} of {}, {} in-flight",
              amountTracker.getAmountDeliveredInDestinationUnits(),
              target.minPaymentAmountInDestinationUnits(),
              amountTracker.getDestinationAmountInFlight()
            );
          }
        }
      });

      this.updateReceiveMax(streamPacketReply);

      return streamPacketReply;
    } finally {
      // Do this even if there's an exception.
      amountTracker.subtractFromSourceAmountScheduled(streamPacketRequest.sourceAmount());
      amountTracker.subtractFromSourceAmountInFlight(streamPacketRequest.sourceAmount());
      amountTracker.subtractFromDestinationAmountInFlight(highEndDestinationAmount);
    }
  }

  /**
   * Update the "receive max" for this payment by comparing the value returned (if any) by the remote receiver in a
   * Stream Frame, but only if its smaller than what has already been seen on the payment path.
   *
   * @param streamPacketReply A {@link StreamPacketReply}.
   */
  @VisibleForTesting
  protected void updateReceiveMax(final StreamPacketReply streamPacketReply) {
    Objects.requireNonNull(streamPacketReply);

    StreamPacketUtils.findStreamMaxMoneyFrames(streamPacketReply.frames())
      .stream()
      .filter(streamMoneyMaxFrame -> streamMoneyMaxFrame.streamId().equals(DEFAULT_STREAM_ID))
      .forEach(streamMoneyMaxFrame -> {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace(
            "Recipient told us the stream has received {} of up to {}",
            streamMoneyMaxFrame.totalReceived(), streamMoneyMaxFrame.receiveMax()
          );
        }

        // Note: totalReceived *can* be greater than receiveMaxFromStreamFrame! (`ilp-protocol-stream` allows receiving
        // 1% more than the receiveMaxFromStreamFrame)
        final UnsignedLong receiveMaxFromStreamFrame = streamMoneyMaxFrame.receiveMax();
        // Remote receive max can only increase
        paymentSharedStateTracker.getAmountTracker().updateRemoteMax(FluentUnsignedLong.of(
          paymentSharedStateTracker.getAmountTracker().getRemoteReceivedMax().orElse(UnsignedLong.ZERO)
          ).orGreater(receiveMaxFromStreamFrame).getValue()
        );
      });
  }

  /**
   * Determines if a packet failed to deliver any value to the receiver. To qualify as a failure, a packet must be a
   * rejection, and also must include a positive {@code deliveryDeficit}.
   *
   * @param deliveryDeficit   An {@link UnsignedLong} representing the shortfall or deficit that is predicted to occur
   *                          if another packet is transmitted to the receiver.
   * @param streamPacketReply A {@link StreamPacketReply} from the payment path.
   *
   * @return {@code true} if the packet failed; {@code false} otherwise.
   */
  @VisibleForTesting
  protected boolean isFailedPacket(
    final UnsignedLong deliveryDeficit, final StreamPacketReply streamPacketReply
  ) {
    Objects.requireNonNull(deliveryDeficit);
    Objects.requireNonNull(streamPacketReply);
    return streamPacketReply.isReject() && FluentUnsignedLong.of(deliveryDeficit).isPositive();
  }

  /**
   * Determines if {@code destinationAmount} is valid, which is defined as being greater-than-or-equal-to {@code
   * minDestinationAmount}.
   *
   * @param destinationAmount    An {@link Optional} of type {@link UnsignedLong}.
   * @param minDestinationAmount An {@link UnsignedLong}.
   *
   * @return {@code true} if the destination amount is valid; {@code false} otherwise.
   */
  @VisibleForTesting
  @SuppressWarnings( {"OptionalUsedAsFieldOrParameterType"})
  protected boolean isDestinationAmountValid(
    final Optional<UnsignedLong> destinationAmount, final UnsignedLong minDestinationAmount
  ) {
    Objects.requireNonNull(destinationAmount);
    Objects.requireNonNull(minDestinationAmount);

    return destinationAmount
      .map(da -> isDestinationAmountValid(da, minDestinationAmount))
      .orElse(false);
  }

  /**
   * Determines if {@code destinationAmount} is valid, which is defined as being greater-than-or-equal-to {@code
   * minDestinationAmount}.
   *
   * @param destinationAmount    An {@link Optional} of type {@link UnsignedLong}.
   * @param minDestinationAmount An {@link UnsignedLong}.
   *
   * @return {@code true} if the destination amount is valid; {@code false} otherwise.
   */
  @VisibleForTesting
  protected boolean isDestinationAmountValid(
    final UnsignedLong destinationAmount, final UnsignedLong minDestinationAmount
  ) {
    Objects.requireNonNull(destinationAmount);
    Objects.requireNonNull(minDestinationAmount);

    return FluentCompareTo.is(destinationAmount).greaterThanEqualTo(minDestinationAmount);
  }

  /**
   * Checks the {@code streamPacketRequest} to determine if the receiver's "receiveMax" is large enough to accommodate
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

    // NOTE: If a remote supports a receiveMax greater-than MaxUInt64, the current Java STREAM implementation will,
    // per IL-RFC-29, decode receiveMax to MaxUInt64. Thus, if the minDeliveryAmount exceeds MaxUInt64, then it will be
    // considered to be an invalid. This is so that receiveMax never exceeds the implementation. In these cases, the
    // implementation should use a smaller source packet value.

    // Is the recipient's advertised `receiveMax` less than the fixed destination amount?
    return amountTracker.getRemoteReceivedMax()
      .filter(remoteReceivedMax ->
        FluentCompareTo.is(target.minPaymentAmountInDestinationUnits()).greaterThan(remoteReceivedMax.bigIntegerValue())
      ).isPresent();
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
      return FluentCompareTo.is(amountTracker.getAmountSentInSourceUnits())
        .equalTo(target.maxPaymentAmountInSenderUnits()) &&
        FluentBigInteger.of(amountTracker.getSourceAmountInFlight()).isNotPositive();
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
    return FluentCompareTo.is(remainingToDeliver).lessThanOrEqualTo(BigInteger.ZERO) && // <-- Everything delivered
      FluentBigInteger.of(amountTracker.getSourceAmountInFlight()).isNotPositive();
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

    return FluentBigInteger
      .of(target.minPaymentAmountInDestinationUnits().subtract(amountTracker.getAmountDeliveredInDestinationUnits()))
      .minusOrZero(BigInteger.ZERO).getValue();
  }

  /**
   * Determine if more packets can be delivered. Note that this calculation does include "in-flight" amounts because
   * those may complete, and if they do, we don't want to duplicate them.
   *
   * @param amountTracker An {@link AmountTracker}.
   * @param target        A {@link PaymentTargetConditions}.
   *
   * @return {@code true} if there is more available to deliver; {@code false} otherwise.
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
   * @return A {@link BigInteger} representing the total amount to send.
   */
  @VisibleForTesting
  protected BigInteger computeAmountAvailableToSend(final PaymentTargetConditions target) {
    Objects.requireNonNull(target);

    return FluentBigInteger.of(
      target.maxPaymentAmountInSenderUnits()
        .subtract(paymentSharedStateTracker.getAmountTracker().getAmountSentInSourceUnits())
        .subtract(paymentSharedStateTracker.getAmountTracker().getSourceAmountScheduled())
        .subtract(paymentSharedStateTracker.getAmountTracker().getSourceAmountInFlight())
    ).minusOrZero(BigInteger.ZERO).getValue();
  }

  /**
   * Compute the total number of units available to be sent in a new prepare packet (in sender's units). This value
   * accounts for all three tracking values (total to deliver, amount already delivered, and amount in-flight).
   *
   * @param target A {@link PaymentTargetConditions}.
   *
   * @return A {@link BigInteger} representing the total amount to send.
   */
  @VisibleForTesting
  protected BigInteger computeAmountAvailableToDeliver(
    final AmountTracker amountTracker, final PaymentTargetConditions target
  ) {
    Objects.requireNonNull(amountTracker);
    Objects.requireNonNull(target);

    return FluentBigInteger.of(
      this.computeRemainingAmountToBeDelivered(amountTracker, target)
        .subtract(amountTracker.getDestinationAmountInFlight())
    ).minusOrZero(BigInteger.ZERO).getValue();
  }

  /**
   * Compute the source delivery limit.
   *
   * @param amountTracker An {@link AmountTracker}.
   * @param target        A {@link PaymentTargetConditions}.
   *
   * @return An {@link UnsignedLong} representing the delivery limit.
   */
  @VisibleForTesting
  protected UnsignedLong computeSourceAmountDeliveryLimit(
    final AmountTracker amountTracker, final PaymentTargetConditions target
  ) {
    Objects.requireNonNull(amountTracker);
    Objects.requireNonNull(target);

    // Total - amtDelivered - amtInFlight
    final BigInteger availableToDeliver = this.computeAmountAvailableToDeliver(amountTracker, target);

    // If this value exceeds UnsignedLong#Max_Value, then cap it at the Max. A sender using this implementation will
    // not be able to send any more money than MAX, and if more value is left to be sent, then that value will merely
    // be sent in another RunLoop execution.
    final UnsignedLong availableToDeliverUL = FluentBigInteger.of(availableToDeliver).orMaxUnsignedLong();

    return this.paymentSharedStateTracker.getExchangeRateTracker()
      .estimateSourceAmount(availableToDeliverUL)
      .highEndEstimate();
  }

  /**
   * Determine if the source delivery limit is invalid.
   *
   * @param amountTracker An {@link AmountTracker}.
   * @param target        A {@link PaymentTargetConditions}.
   *
   * @return {@code true} if the delivery limit is invalid; {@code false} otherwise.
   */
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
    // Using UnsignedLong is appropriate here because the ExchangeRateTracker only tracks delivered packet amounts using
    // UnsignedLongs because nothing larger can fit in an ILPv4 packet.
    return paymentSharedStateTracker.getExchangeRateTracker().estimateDestinationAmount(sourceAmount).lowEndEstimate();
  }

  /**
   * Compute the minimum destination amount for a packet based upon the supplied {@code sourcePacketAmount} and {@code
   * minExchangeRate}. The returned value is the smallest packet value that we expect the destination to accept. Note
   * that if an overflow condition occurs, this method will return {@link UnsignedLong#ZERO}, which should have the
   * effect of aborting a particular run-loop iteration with the specified packet amounts (in-favor of smaller amounts
   * if possible).
   *
   * @param sourcePacketAmount An {@link UnsignedLong} representing the original source amount of a payment, in sender's
   *                           units.
   * @param minExchangeRate    An {@link BigDecimal} representing the minimum acceptable exchange rate, as discovered by
   *                           the path and overall payment input parameters.
   *
   * @return An {@link UnsignedLong} representing the minimum destination amount.
   */
  @VisibleForTesting
  protected UnsignedLong computeMinDestinationPacketAmount(
    final UnsignedLong sourcePacketAmount, final Ratio minExchangeRate
  ) {
    Objects.requireNonNull(sourcePacketAmount);
    Objects.requireNonNull(minExchangeRate);
    return FluentUnsignedLong.of(sourcePacketAmount).timesCeilOrZero(minExchangeRate).getValue();
  }

  /**
   * Compute the delivery deficit, which is the delta between what we expect will be delivered, and the minimum
   * destination amount. If this number is positive (i.e., greater-than-zero), it indicates that we likely won't be able
   * to deliver the full amount of a payment, because we need the estimate, but it's less than the minimum acceptable
   * amount, which means nothing will get delivered.
   *
   * @param minDestinationPacketAmount       An {@link UnsignedLong} representing the minimum acceptable destination
   *                                         amount, in destination units.
   * @param estimatedDestinationPacketAmount An {@link UnsignedLong} representing the estimated amount that will be
   *                                         delivered, in destination units.
   *
   * @return An {@link UnsignedLong} representing the anticipated delivery deficit, if any. Note that this operation is
   *   dehydrating, so any negative number will be returned as {@link UnsignedLong#ZERO}.
   */
  @VisibleForTesting
  protected UnsignedLong computePacketDeliveryDeficitForNextState(
    final UnsignedLong minDestinationPacketAmount, final UnsignedLong estimatedDestinationPacketAmount
  ) {
    Objects.requireNonNull(minDestinationPacketAmount);
    Objects.requireNonNull(estimatedDestinationPacketAmount);

    return FluentUnsignedLong.of(minDestinationPacketAmount).minusOrZero(estimatedDestinationPacketAmount).getValue();
  }

  /**
   * Computes the delivery deficit for {@link #doFilter(StreamPacketRequest, StreamPacketFilterChain)}.
   *
   * @param sourceAmount         An {@link UnsignedLong} representing the original source amount in the prepare packet.
   * @param minExchangeRate      A {@link Ratio} representing the minimum exchange rate to use for any FX calculations.
   * @param minDestinationAmount An {@link UnsignedLong} representing the minimum destination amount allowed by the
   *                             receiver.
   *
   * @return An {@link UnsignedLong} representing the delivery deficit (or zero if the result would be negative).
   */
  @VisibleForTesting
  protected UnsignedLong computePacketDeliveryDeficitForDoFilter(
    final UnsignedLong sourceAmount, final Ratio minExchangeRate, final UnsignedLong minDestinationAmount
  ) {
    Objects.requireNonNull(sourceAmount);
    Objects.requireNonNull(minExchangeRate);
    Objects.requireNonNull(minDestinationAmount);
    final UnsignedLong baselineMinDestinationAmount = this
      .computeMinDestinationPacketAmount(sourceAmount, minExchangeRate);
    return FluentUnsignedLong.of(baselineMinDestinationAmount).minusOrZero(minDestinationAmount).getValue();
  }

  /**
   * Determines if the payment will complete by summing the amount delivered and amount in flight to see if there's more
   * to deliver.
   *
   * @param amountTracker                    A {@link AmountTracker}.
   * @param target                           A {@link PaymentTargetConditions}.
   * @param availableToSend                  A {@link BigInteger} representing the amount remaining or available to be
   *                                         sent (in sender units) in one or more packets (i.e., the overall amount
   *                                         left to be send).
   * @param sourcePacketAmount               A {@link UnsignedLong} representing the overall source amount to be sent
   *                                         (in sender units) in the next packet.
   * @param estimatedDestinationPacketAmount A {@link UnsignedLong} representing the estimated amount that will be
   *                                         delivered to the receiver, in destination units, for the next packet.
   *
   * @return {@code true} if the payment will complete; {@code false} if it will not.
   */
  @VisibleForTesting
  protected boolean willPaymentComplete(
    final AmountTracker amountTracker,
    final PaymentTargetConditions target,
    final BigInteger availableToSend,
    final UnsignedLong sourcePacketAmount,
    final UnsignedLong estimatedDestinationPacketAmount
  ) {
    return target.paymentType() == PaymentType.FIXED_SEND ?
      sourcePacketAmount.bigIntegerValue().equals(availableToSend)
      : FluentCompareTo.is(
        amountTracker.getAmountDeliveredInDestinationUnits()
          .add(amountTracker.getDestinationAmountInFlight())
          .add(estimatedDestinationPacketAmount.bigIntegerValue())
      ).greaterThanEqualTo(target.minPaymentAmountInDestinationUnits());
  }

}
