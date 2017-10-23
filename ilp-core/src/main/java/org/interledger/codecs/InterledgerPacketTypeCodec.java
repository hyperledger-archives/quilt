package org.interledger.codecs;

import org.interledger.codecs.packettypes.InterledgerPacketType;

/**
 * An implementation of {@link Codec} that reads and writes instances of
 * {@link InterledgerPacketType}.
 */
public interface InterledgerPacketTypeCodec extends Codec<InterledgerPacketType> {

}
