package org.interledger.stream;

import com.google.common.primitives.UnsignedLong;

import java.util.Optional;

public interface PaymentTracker {

  UnsignedLong getOriginalAmount();

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
