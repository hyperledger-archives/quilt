package org.interledger.cryptoconditions;

import java.util.EnumSet;

/**
 * Compound conditions extend regular conditions by defining the subtypes of all sub-conditions.
 * This set MUST exclude the type of this condition.
 * 
 * @author adrianhopebailie
 *
 */
public interface CompoundCondition extends Condition {

  EnumSet<CryptoConditionType> getSubtypes();

}
