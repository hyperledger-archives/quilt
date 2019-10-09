package org.interledger.stream.sender;

import org.interledger.stream.Denomination;
import org.interledger.stream.PacketAmounts;
import org.interledger.stream.PaymentTracker;
import org.interledger.stream.StreamUtils;
import org.interledger.stream.calculators.ExchangeRateCalculator;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.UnsignedLong;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class FixedSenderAmountPaymentTracker implements PaymentTracker {

  // The original amount, in sender's units, to send
  private final UnsignedLong amountToSend;
  // The amount, in sender's units, left to send (i.e., the unsent amount). On rare error occasions, this value
  // _could_ diverge from 1-sentAmount.
  private final AtomicReference<UnsignedLong> amountLeftToSend;
  // The amount, in sender's units, that was sent.
  private final AtomicReference<UnsignedLong> sentAmount;
  // The amount, in receiver's units, that was actually delivered to the receiver.
  private final AtomicReference<UnsignedLong> deliveredAmount;

  private final ExchangeRateCalculator rateCalculator;

  public FixedSenderAmountPaymentTracker(UnsignedLong amountToSend, ExchangeRateCalculator rateCalculator) {
    this.amountToSend = amountToSend;
    amountLeftToSend = new AtomicReference<>(amountToSend);
    this.rateCalculator = rateCalculator;
    sentAmount = new AtomicReference<>(UnsignedLong.ZERO);
    deliveredAmount = new AtomicReference<>(UnsignedLong.ZERO);
  }

  @Override
  public UnsignedLong getOriginalAmount() {
    return amountToSend;
  }

  @Override
  public UnsignedLong getOriginalAmountLeft() {
    return amountLeftToSend.get();
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
  public PacketAmounts getSendPacketAmounts(UnsignedLong congestionLimit,
                                            Denomination sendDenomination,
                                            Optional<Denomination> receiverDenomination) {
    final UnsignedLong packetAmountToSend = StreamUtils.min(amountLeftToSend.get(), congestionLimit);
    return PacketAmounts.of()
        .minimumAmountToAccept(rateCalculator.calculateMinAmountToAccept(packetAmountToSend, sendDenomination, receiverDenomination))
        .amountToSend(packetAmountToSend)
        .build();
  }

  @Override
  public void auth(PacketAmounts packetAmounts) {
    this.amountLeftToSend.getAndUpdate(sourceAmount -> sourceAmount.minus(packetAmounts.getAmountToSend()));
  }

  @Override
  public void rollback(PacketAmounts packetAmounts) {
    this.amountLeftToSend.getAndUpdate(sourceAmount -> sourceAmount.plus(packetAmounts.getAmountToSend()));
  }

  @Override
  public void commit(PacketAmounts packetAmounts) {
    this.deliveredAmount.getAndUpdate(currentAmount -> currentAmount.plus(packetAmounts.getMinimumAmountToAccept()));
    this.sentAmount.getAndUpdate(currentAmount -> currentAmount.plus(packetAmounts.getAmountToSend()));
  }

  @Override
  public boolean moreToSend() {
    return this.sentAmount.get().compareTo(this.amountToSend) < 0;
  }

  @VisibleForTesting
  void setSentAmount(UnsignedLong sentAmount) {
    this.sentAmount.set(sentAmount);
  }

}
