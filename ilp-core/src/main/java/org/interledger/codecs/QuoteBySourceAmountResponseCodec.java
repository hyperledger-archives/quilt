package org.interledger.codecs;

import org.interledger.codecs.packettypes.InterledgerPacketType;
import org.interledger.codecs.packettypes.QuoteBySourceAmountResponsePacketType;
import org.interledger.ilqp.QuoteBySourceAmountResponse;

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