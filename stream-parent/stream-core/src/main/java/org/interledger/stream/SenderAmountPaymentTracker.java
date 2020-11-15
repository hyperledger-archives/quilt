package org.interledger.stream;

import com.google.common.primitives.UnsignedLong;
import org.interledger.fx.Denomination;

/**
 * An extension of {@link PaymentTracker} that defines the {@link #getOriginalAmount()} to be in the sender's units.
 *
 * @deprecated TODO: Remove
 */
@Deprecated
public interface SenderAmountPaymentTracker extends PaymentTracker<SenderAmountMode> {

  @Override
  default SenderAmountMode getOriginalAmountMode() {
    return SenderAmountMode.SENDER_AMOUNT;
  }

  /**
   * ReceiverDenomination is generally not required in a Payment Tracking in SENDER_AMOUNT mode.
   *
   * @param congestionLimit      An {@link UnsignedLong} representing the number of units that the congestion controller
   *                             has indicated should be sent.
   * @param senderDenomination   A {@link Denomination} representing the asset information for the sender, in order to
   *                             compute path-exchange-rates.
   * @param receiverDenomination A {@link Denomination} representing the asset information for the receiver, in order to
   *                             compute path-exchange-rates.
   * @return
   */
  @Override
  default PrepareAmounts getSendPacketAmounts(
    UnsignedLong congestionLimit, Denomination senderDenomination, Denomination receiverDenomination
  ) {
    return getSendPacketAmounts(congestionLimit, senderDenomination);
  }
}
