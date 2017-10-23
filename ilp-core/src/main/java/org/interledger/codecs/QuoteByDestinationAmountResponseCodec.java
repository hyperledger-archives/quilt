package org.interledger.codecs;

import org.interledger.codecs.packettypes.InterledgerPacketType;
import org.interledger.codecs.packettypes.QuoteByDestinationAmountResponsePacketType;
import org.interledger.ilqp.QuoteByDestinationAmountResponse;

/**
 * An implementation of {@link Codec} that reads and writes instances of {@link
 * QuoteByDestinationAmountResponse}.
 */
public interface QuoteByDestinationAmountResponseCodec extends
    InterledgerPacketCodec<QuoteByDestinationAmountResponse> {

  InterledgerPacketType TYPE = new QuoteByDestinationAmountResponsePacketType();

  @Override
  default InterledgerPacketType getTypeId() {
    return TYPE;
  }

}
