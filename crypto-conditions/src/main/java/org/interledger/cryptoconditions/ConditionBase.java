package org.interledger.cryptoconditions;

import org.interledger.cryptoconditions.der.DerEncodingException;

/**
 * This class provides shared, concrete logic for all conditions.
 */
public abstract class ConditionBase<C extends Condition> implements Condition {

  /**
   * <p>An implementation of {@link Comparable#compareTo(Object)} to conform to the {@link
   * Comparable} interface.</p>
   *
   * <p>This implementation merely loops through the bytes of each encoded condition and returns the
   * result of that comparison.</p>
   *
   * @param that A {@link Condition} to compare against this condition.
   *
   * @return a negative integer, zero, or a positive integer as this object is less than, equal to,
   *     or greater than the specified object.
   */
  @Override
  public final int compareTo(Condition that) {
    try {
      byte[] c1encoded = CryptoConditionWriter.writeCondition(this);
      byte[] c2encoded = CryptoConditionWriter.writeCondition(that);

      int minLength = Math.min(c1encoded.length, c2encoded.length);
      for (int i = 0; i < minLength; i++) {
        int result = Integer.compareUnsigned(c1encoded[i], c2encoded[i]);
        if (result != 0) {
          return result;
        }
      }
      return c1encoded.length - c2encoded.length;

    } catch (DerEncodingException e) {
      throw new RuntimeException(e);
    }
  }
}
