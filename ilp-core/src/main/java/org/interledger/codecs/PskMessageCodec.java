package org.interledger.codecs;

import org.interledger.psk.PskMessage;

/**
 * An implementation of {@link Codec} that reads and writes instances of {@link PskMessage}.
 */
public interface PskMessageCodec extends Codec<PskMessage> {

}
