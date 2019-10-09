package org.interledger.stream;

import com.google.common.primitives.UnsignedLong;

import java.util.Optional;

/**
 * Defines how to track a payment while considering the amount sent vs amount received, allowing room for
 * path-exchange-rate fluctuations and implementation-defined rules relating to whether or not to continue a payment.
 */
public interface PaymentTracker<T extends SenderAmountMode> {

  UnsignedLong getOriginalAmount();

  /**
   * Returns the {@link SenderAmountMode} for this payment tracker.
   *
   * @return A {@link SenderAmountMode} that indicates the meaning of {@link #getOriginalAmount()}.
   */
  T getOriginalAmountMode();

  UnsignedLong getOriginalAmountLeft();

  UnsignedLong getAmountSent();

  UnsignedLong getDeliveredAmount();

  PrepareAmounts getSendPacketAmounts(UnsignedLong congestionLimit,
      Denomination sendDenomination,
      Optional<Denomination> receiverDenomination);

  void auth(PrepareAmounts prepareAmounts);

  void rollback(PrepareAmounts prepareAmounts, boolean packetRejected);

  void commit(PrepareAmounts prepareAmounts, UnsignedLong deliveredAmount);

  boolean moreToSend();


  default boolean successful() {
    return !moreToSend();
  }
}
