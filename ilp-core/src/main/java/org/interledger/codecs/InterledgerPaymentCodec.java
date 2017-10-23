package org.interledger.codecs;

import org.interledger.codecs.packettypes.InterledgerPacketType;
import org.interledger.codecs.packettypes.PaymentPacketType;
import org.interledger.ilp.InterledgerPayment;

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
