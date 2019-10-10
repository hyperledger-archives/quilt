package org.interledger.stream.sender;

import static org.interledger.stream.FluentCompareTo.is;

import org.interledger.stream.Denomination;
import org.interledger.stream.PrepareAmounts;
import org.interledger.stream.ReceiverAmountPaymentTracker;
import org.interledger.stream.SenderAmountMode;
import org.interledger.stream.StreamUtils;
import org.interledger.stream.calculators.ExchangeRateCalculator;
import org.interledger.stream.calculators.NoOpExchangeRateCalculator;

import com.google.common.primitives.UnsignedLong;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class FixedReceiverAmountPaymentTracker implements ReceiverAmountPaymentTracker {

  // The original amount, in receiver's units, to send
  private final UnsignedLong amountToDeliver;
  // The amount, in sender's units, left to send (i.e., the unsent amount). On rare error occasions, this value
  // _could_ diverge from 1-sentAmount.
  private final AtomicReference<UnsignedLong> amountLeftToDeliver;
  // The amount, in sender's units, that was sent.
  private final AtomicReference<UnsignedLong> sentAmount;
  // The amount, in receiver's units, that was actually delivered to the receiver.
  private final AtomicReference<UnsignedLong> deliveredAmount;

  private final ExchangeRateCalculator rateCalculator;

  /**
   * Required-args Constructor.
   *
   * @param amountToDeliver An {@link UnsignedLong} representing the amount to send to the receiver, denominated in the
   *                        receiver's units.
   */
  public FixedReceiverAmountPaymentTracker(final UnsignedLong amountToDeliver) {
    this(amountToDeliver, new NoOpExchangeRateCalculator());
  }

  /**
   * Required-args Constructor.
   *
   * @param amountToDeliver An {@link UnsignedLong} representing the amount to send, in the receiver's units.
   * @param rateCalculator  An {@link ExchangeRateCalculator} that informs this tracker which amounts to use is
   *                        subsequent operations, depending on market exchange rates, observed path rates, and possibly
   *                        other data.
   */
  public FixedReceiverAmountPaymentTracker(
      final UnsignedLong amountToDeliver, final ExchangeRateCalculator rateCalculator
  ) {
    this.amountToDeliver = Objects.requireNonNull(amountToDeliver);
    this.rateCalculator = Objects.requireNonNull(rateCalculator);

    amountLeftToDeliver = new AtomicReference<>(amountToDeliver);
    sentAmount = new AtomicReference<>(UnsignedLong.ZERO);
    deliveredAmount = new AtomicReference<>(UnsignedLong.ZERO);
  }

  @Override
  public UnsignedLong getOriginalAmount() {
    return amountToDeliver;
  }

  @Override
  public SenderAmountMode getOriginalAmountMode() {
    return SenderAmountMode.RECEIVER_AMOUNT;
  }

  @Override
  public UnsignedLong getOriginalAmountLeft() {
    return amountLeftToDeliver.get();
  }

  @Override
  public UnsignedLong getAmountSent() {
    return sentAmount.get();
  }

  @Override
  public UnsignedLong getDeliveredAmount() {
    return deliveredAmount.get();
  }

  @Override
  public PrepareAmounts getSendPacketAmounts(
      UnsignedLong congestionLimit, Denomination sendDenomination, Optional<Denomination> receiverDenomination
  ) {
    if (congestionLimit.equals(UnsignedLong.ZERO) || amountLeftToDeliver.get().equals(UnsignedLong.ZERO)) {
      return PrepareAmounts.builder().amountToSend(UnsignedLong.ZERO).minimumAmountToAccept(UnsignedLong.ZERO).build();
    }
    UnsignedLong amountToSendInSenderUnits =
        rateCalculator.calculateAmountToSend(amountLeftToDeliver.get(), sendDenomination, receiverDenomination.get());
    final UnsignedLong packetAmountToSend = StreamUtils.max(StreamUtils.min(amountToSendInSenderUnits, congestionLimit),
        UnsignedLong.ONE);
    UnsignedLong minAmountToAcceptInReceiverUnits =
        rateCalculator.calculateMinAmountToAccept(packetAmountToSend, sendDenomination, receiverDenomination);
    return PrepareAmounts.builder()
        .minimumAmountToAccept(minAmountToAcceptInReceiverUnits)
        .amountToSend(packetAmountToSend)
        .build();
  }

  @Override
  public void auth(PrepareAmounts prepareAmounts) {
    reduceAmountLeftToDeliver(prepareAmounts.getMinimumAmountToAccept());
  }

  private void reduceAmountLeftToDeliver(UnsignedLong amountToReduce) {
    this.amountLeftToDeliver.getAndUpdate(sourceAmount -> {
      if (is(sourceAmount).lessThan(amountToReduce)) {
        // we've overdelivered so just set the amountToDeliver to 0 since UnsignedLong cannot go negative
        return UnsignedLong.ZERO;
      } else {
        return sourceAmount.minus(amountToReduce);
      }
    });
  }

  @Override
  public void rollback(PrepareAmounts prepareAmounts, boolean packetRejected) {
    this.amountLeftToDeliver.getAndUpdate(sourceAmount -> sourceAmount.plus(prepareAmounts.getMinimumAmountToAccept()));
  }

  @Override
  public void commit(PrepareAmounts prepareAmounts, UnsignedLong deliveredAmount) {
    if (is(prepareAmounts.getMinimumAmountToAccept()).lessThan(deliveredAmount)) {
      UnsignedLong overrage = deliveredAmount.minus(prepareAmounts.getMinimumAmountToAccept());
      reduceAmountLeftToDeliver(overrage);
    }
    this.deliveredAmount.getAndUpdate(currentAmount -> currentAmount.plus(deliveredAmount));
    this.sentAmount.getAndUpdate(currentAmount -> currentAmount.plus(prepareAmounts.getAmountToSend()));
  }

  @Override
  public boolean moreToSend() {
    return is(deliveredAmount.get()).lessThan(this.amountToDeliver);
  }

}
