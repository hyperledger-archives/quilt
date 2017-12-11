package org.interledger.ilp;

import org.interledger.cryptoconditions.Fulfillment;

import org.hyperledger.quilt.codecs.framework.Codec;

/**
 * An implementation of {@link Codec} that reads and writes instances of {@link Fulfillment}.
 */
public interface FulfillmentCodec extends Codec<Fulfillment> {

}
