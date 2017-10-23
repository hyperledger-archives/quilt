package org.interledger.codecs;

import org.interledger.codecs.packettypes.InterledgerPacketType;
import org.interledger.codecs.packettypes.QuoteByDestinationAmountRequestPacketType;
import org.interledger.ilqp.QuoteByDestinationAmountRequest;

/**
 * An implementation of {@link Codec} that reads and writes instances of {@link
 * QuoteByDestinationAmountRequest}.
 */
public interface QuoteByDestinationAmountRequestCodec extends
    InterledgerPacketCodec<QuoteByDestinationAmountRequest> {

  InterledgerPacketType TYPE = new QuoteByDestinationAmountRequestPacketType();

  @Override
  default InterledgerPacketType getTypeId() {
    return TYPE;
  }

}
