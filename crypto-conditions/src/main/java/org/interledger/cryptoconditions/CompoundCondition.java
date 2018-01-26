package org.interledger.cryptoconditions;

import java.util.EnumSet;

/**
 * Compound conditions extend regular conditions by defining the subtypes any sub-conditions.
 */
public interface CompoundCondition extends Condition {

  /**
   * <p>Accessor for the sub-types of a compound condition.</p>
   *
   * <p>Note that this set MUST exclude the type of this condition. </p>
   *
   * @return An instance of {@link EnumSet} of type {@link CryptoConditionType}.
   */
  EnumSet<CryptoConditionType> getSubtypes();

}
