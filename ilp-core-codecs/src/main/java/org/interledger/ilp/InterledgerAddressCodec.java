package org.interledger.ilp;

import org.interledger.InterledgerAddress;

import org.hyperledger.quilt.codecs.framework.Codec;

/**
 * An implementation of {@link Codec} that reads and writes instances of {@link InterledgerAddress}.
 */
public interface InterledgerAddressCodec extends Codec<InterledgerAddress> {

}
