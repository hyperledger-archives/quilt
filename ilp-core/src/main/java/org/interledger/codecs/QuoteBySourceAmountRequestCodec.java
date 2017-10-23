package org.interledger.codecs;

import org.interledger.codecs.packettypes.InterledgerPacketType;
import org.interledger.codecs.packettypes.QuoteBySourceAmountRequestPacketType;
import org.interledger.ilqp.QuoteBySourceAmountRequest;

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
