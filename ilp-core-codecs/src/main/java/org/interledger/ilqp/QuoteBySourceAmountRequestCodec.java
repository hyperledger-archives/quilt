package org.interledger.ilqp;

import org.interledger.InterledgerPacketType;
import org.interledger.codecs.InterledgerPacketCodec;
import org.interledger.ilqp.packets.QuoteBySourceAmountRequestPacketType;

import org.hyperledger.quilt.codecs.framework.Codec;

/**
 * An implementation of {@link Codec} that reads and writes instances of {@link
 * QuoteBySourceAmountRequest}.
 */
public interface QuoteBySourceAmountRequestCodec
    extends InterledgerPacketCodec<QuoteBySourceAmountRequest> {

  InterledgerPacketType TYPE = new QuoteBySourceAmountRequestPacketType();

  @Override
  default InterledgerPacketType getTypeId() {
    return TYPE;
  }

}
