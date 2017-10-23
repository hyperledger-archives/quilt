package org.interledger.codecs;

import org.interledger.cryptoconditions.Condition;

/**
 * An implementation of {@link Codec} that reads and writes instances of {@link Condition}.
 */
public interface ConditionCodec extends Codec<Condition> {

}
