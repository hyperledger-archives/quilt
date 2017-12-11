package org.interledger.ilqp;

import org.interledger.InterledgerPacketType;
import org.interledger.codecs.InterledgerPacketCodec;
import org.interledger.ilqp.packets.QuoteByDestinationAmountRequestPacketType;

import org.hyperledger.quilt.codecs.framework.Codec;

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
