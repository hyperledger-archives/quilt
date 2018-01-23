package org.interledger.transport.psk.codecs;

import org.interledger.transport.psk.PskMessage;

import org.hyperledger.quilt.codecs.framework.Codec;

/**
 * An implementation of {@link Codec} that reads and writes instances of {@link PskMessage}.
 */
public interface PskMessageCodec extends Codec<PskMessage> {

}
