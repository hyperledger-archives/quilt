package org.interledger.ilp;

import org.interledger.InterledgerPacketType;
import org.interledger.codecs.InterledgerPacketCodec;
import org.interledger.ilp.packets.PaymentPacketType;

import org.hyperledger.quilt.codecs.framework.Codec;

/**
 * An implementation of {@link Codec} that reads and writes instances of {@link InterledgerPayment}.
 */
public interface InterledgerPaymentCodec extends InterledgerPacketCodec<InterledgerPayment> {

  InterledgerPacketType TYPE = new PaymentPacketType();

  @Override
  default InterledgerPacketType getTypeId() {
    return TYPE;
  }
}
