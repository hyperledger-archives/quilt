package org.interledger.stream;

/**
 * An extension of {@link PaymentTracker} that defines the {@link #getOriginalAmount()} to be in the receiver's units.
 */
public interface ReceiverAmountPaymentTracker extends PaymentTracker<SenderAmountMode> {

  @Override
  default SenderAmountMode getOriginalAmountMode() {
    return SenderAmountMode.RECEIVER_AMOUNT;
  }

}
