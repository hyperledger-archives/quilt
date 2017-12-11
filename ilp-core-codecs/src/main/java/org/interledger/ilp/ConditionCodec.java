package org.interledger.ilp;

import org.interledger.cryptoconditions.Condition;

import org.hyperledger.quilt.codecs.framework.Codec;

/**
 * An implementation of {@link Codec} that reads and writes instances of {@link Condition}.
 */
public interface ConditionCodec extends Codec<Condition> {

}
