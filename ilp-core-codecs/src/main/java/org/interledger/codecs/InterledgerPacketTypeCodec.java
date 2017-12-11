package org.interledger.codecs;

import org.interledger.InterledgerPacketType;

import org.hyperledger.quilt.codecs.framework.Codec;

/**
 * An implementation of {@link Codec} that reads and writes instances of
 * {@link InterledgerPacketType}.
 */
public interface InterledgerPacketTypeCodec extends Codec<InterledgerPacketType> {

}
