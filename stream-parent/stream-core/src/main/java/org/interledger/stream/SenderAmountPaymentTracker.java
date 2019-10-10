package org.interledger.stream;

/**
 * An extension of {@link PaymentTracker} that defines the {@link #getOriginalAmount()} to be in the sender's units.
 */
public interface SenderAmountPaymentTracker extends PaymentTracker<SenderAmountMode> {

  @Override
  default SenderAmountMode getOriginalAmountMode() {
    return SenderAmountMode.SENDER_AMOUNT;
  }

}
