package org.interledger.codecs;

import org.interledger.codecs.packettypes.InterledgerErrorPacketType;
import org.interledger.codecs.packettypes.InterledgerPacketType;
import org.interledger.ilp.InterledgerProtocolError;

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
