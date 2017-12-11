package org.interledger.ilp;

import org.interledger.InterledgerPacketType;
import org.interledger.codecs.InterledgerPacketCodec;
import org.interledger.ilp.packets.InterledgerErrorPacketType;

import org.hyperledger.quilt.codecs.framework.Codec;

/**
 * An implementation of {@link Codec} that reads and writes instances of {@link
 * InterledgerProtocolError}.
 */
public interface InterledgerProtocolErrorCodec extends
    InterledgerPacketCodec<InterledgerProtocolError> {

  InterledgerPacketType TYPE = new InterledgerErrorPacketType();

  @Override
  default InterledgerPacketType getTypeId() {
    return TYPE;
  }
}
