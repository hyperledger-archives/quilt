package org.interledger.stream;

import com.google.common.primitives.UnsignedLong;

import java.util.Optional;

public interface PaymentTracker {

  UnsignedLong getOriginalAmount();

  UnsignedLong getOriginalAmountLeft();

  UnsignedLong getAmountSent();

  UnsignedLong getDeliveredAmount();

  PacketAmounts getSendPacketAmounts(UnsignedLong congestionLimit,
                                     Denomination sendDenomination,
                                     Optional<Denomination> receiverDenomination);

  void auth(PacketAmounts build);

  void rollback(PacketAmounts packetAmounts);

  void commit(PacketAmounts packetAmounts);

  boolean moreToSend();

}
