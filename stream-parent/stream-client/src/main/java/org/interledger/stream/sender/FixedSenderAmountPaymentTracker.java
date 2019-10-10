package org.interledger.stream.sender;

import static org.interledger.stream.FluentCompareTo.is;

import org.interledger.stream.Denomination;
import org.interledger.stream.PrepareAmounts;
import org.interledger.stream.SenderAmountPaymentTracker;
import org.interledger.stream.StreamUtils;
import org.interledger.stream.calculators.ExchangeRateCalculator;
import org.interledger.stream.calculators.NoOpExchangeRateCalculator;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.UnsignedLong;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An implementation of {@link SenderAmountPaymentTracker} that uses a fixed amount to send, denominated in the sender's
 * units, as reflected in the `amountToDeliver`.
 */
public class FixedSenderAmountPaymentTracker implements SenderAmountPaymentTracker {

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

  /**
   * Required-args Constructor.
   *
   * @param amountToSend An {@link UnsignedLong} representing the amount to send to the receiver, denominated in the
   *                     sender's units.
   */
  public FixedSenderAmountPaymentTracker(final UnsignedLong amountToSend) {
    this(amountToSend, new NoOpExchangeRateCalculator());
  }

  /**
   * Required-args Constructor.
   *
   * @param amountToSend   An {@link UnsignedLong} representing the amount to send, in the sender's units.
   * @param rateCalculator An {@link ExchangeRateCalculator} that informs this tracker which amounts to use is
   *                       subsequent operations, depending on market exchange rates, observed path rates, and possibly
   *                       other data.
   */
  public FixedSenderAmountPaymentTracker(final UnsignedLong amountToSend, final ExchangeRateCalculator rateCalculator) {
    this.amountToSend = Objects.requireNonNull(amountToSend);
    this.rateCalculator = Objects.requireNonNull(rateCalculator);

    amountLeftToSend = new AtomicReference<>(amountToSend);
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
  public UnsignedLong getDeliveredAmountInSenderUnits() {
    return sentAmount.get();
  }

  @Override
  public UnsignedLong getDeliveredAmountInReceiverUnits() {
    return deliveredAmount.get();
  }

  @Override
  public PrepareAmounts getSendPacketAmounts(
      final UnsignedLong congestionLimit,
      final Denomination senderDenomination,
      final Optional<Denomination> receiverDenomination
  ) {
    Objects.requireNonNull(congestionLimit);
    Objects.requireNonNull(senderDenomination);
    Objects.requireNonNull(receiverDenomination);

    final UnsignedLong packetAmountToSend = StreamUtils.min(amountLeftToSend.get(), congestionLimit);
    return PrepareAmounts.builder()
        .minimumAmountToAccept(
            rateCalculator.calculateMinAmountToAccept(packetAmountToSend, senderDenomination, receiverDenomination))
        .amountToSend(packetAmountToSend)
        .build();
  }

  @Override
  public boolean auth(final PrepareAmounts prepareAmounts) {
    Objects.requireNonNull(prepareAmounts);

    if (is(sentAmount.get().plus(prepareAmounts.getAmountToSend())).greaterThan(amountToSend) ||
        is(amountLeftToSend.get()).lessThan(prepareAmounts.getAmountToSend())) {
      return false;
    } else {
      this.amountLeftToSend.getAndUpdate(sourceAmount -> sourceAmount.minus(prepareAmounts.getAmountToSend()));
      return true;
    }

  }

  @Override
  public void rollback(final PrepareAmounts prepareAmounts, final boolean packetRejected) {
    Objects.requireNonNull(prepareAmounts);
    Objects.requireNonNull(packetRejected);
    this.amountLeftToSend.getAndUpdate(sourceAmount -> sourceAmount.plus(prepareAmounts.getAmountToSend()));
  }

  @Override
  public void commit(final PrepareAmounts prepareAmounts, final UnsignedLong deliveredAmount) {
    Objects.requireNonNull(prepareAmounts);
    Objects.requireNonNull(deliveredAmount);
    this.deliveredAmount.getAndUpdate(currentAmount -> currentAmount.plus(deliveredAmount));
    this.sentAmount.getAndUpdate(currentAmount -> currentAmount.plus(prepareAmounts.getAmountToSend()));
  }

  @Override
  public boolean moreToSend() {
    return is(this.sentAmount.get()).lessThan(this.amountToSend);
  }

  @VisibleForTesting
  void setSentAmount(UnsignedLong sentAmount) {
    this.sentAmount.set(sentAmount);
  }

}
