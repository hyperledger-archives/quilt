package org.interledger.stream;

import com.google.common.primitives.UnsignedLong;

/**
 * An extension of {@link PaymentTracker} that defines the {@link #getOriginalAmount()} to be in the receiver's units.
 *
 * @deprecated This class will be removed in a future version in-favor of ILP Pay functionality.
 */
@Deprecated
public interface ReceiverAmountPaymentTracker extends PaymentTracker<SenderAmountMode> {

  @Override
  default SenderAmountMode getOriginalAmountMode() {
    return SenderAmountMode.RECEIVER_AMOUNT;
  }

  @Override
  default PrepareAmounts getSendPacketAmounts(UnsignedLong congestionLimit, Denomination senderDenomination) {
    throw new RuntimeException(
      "Implementations of ReceiverAmountPaymentTracker require a Denomination. Call the getSendPacketAmounts() that"
        + " requires a receiverDenomination instead."
    );
  }
}
