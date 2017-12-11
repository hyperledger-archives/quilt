package org.interledger.ilqp;

import org.interledger.InterledgerPacketType;
import org.interledger.codecs.InterledgerPacketCodec;
import org.interledger.ilqp.packets.QuoteBySourceAmountResponsePacketType;

import org.hyperledger.quilt.codecs.framework.Codec;

/**
 * An implementation of {@link Codec} that reads and writes instances of {@link
 * QuoteBySourceAmountResponse}.
 */
public interface QuoteBySourceAmountResponseCodec
    extends InterledgerPacketCodec<QuoteBySourceAmountResponse> {

  InterledgerPacketType TYPE = new QuoteBySourceAmountResponsePacketType();

  @Override
  default InterledgerPacketType getTypeId() {
    return TYPE;
  }

}