package org.interledger.ilqp;

import org.interledger.InterledgerPacketType;
import org.interledger.codecs.InterledgerPacketCodec;
import org.interledger.ilqp.packets.QuoteByDestinationAmountResponsePacketType;

import org.hyperledger.quilt.codecs.framework.Codec;

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
