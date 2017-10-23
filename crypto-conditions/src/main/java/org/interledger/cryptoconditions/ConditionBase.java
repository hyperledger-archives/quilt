package org.interledger.cryptoconditions;

import org.interledger.cryptoconditions.der.DerEncodingException;

import java.util.Objects;

/**
 * This class provides shared, concrete logic for all conditions.
 */
public abstract class ConditionBase implements Condition {

  private final CryptoConditionType type;
  private final long cost;

  /**
   * Default internal constructor for all conditions. Sub-classes must statically calculate the cost
   * of a condition and call this constructor with the correct cost value.
   *
   * @param type The type of this condition.
   * @param cost the cost value for this condition.
   */
  protected ConditionBase(final CryptoConditionType type, final long cost) {
    this.type = Objects.requireNonNull(type);

    if (cost < 0) {
      throw new IllegalArgumentException("Cost must be positive!");
    }
    this.cost = cost;
  }

  @Override
  public final CryptoConditionType getType() {
    return this.type;
  }

  @Override
  public final long getCost() {
    return cost;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }

    ConditionBase that = (ConditionBase) object;

    if (cost != that.cost) {
      return false;
    }
    return type == that.type;
  }

  @Override
  public int hashCode() {
    int result = type.hashCode();
    result = 31 * result + (int) (cost ^ (cost >>> 32));
    return result;
  }

  /**
   * Overrides the default {@link java.lang.Object#toString()} and returns the result of {@link
   * CryptoConditionUri#toUri(Condition)} as a string.
   */
  @Override
  public final String toString() {
    return CryptoConditionUri.toUri(this).toString();
  }

  /**
   * <p>An implementation of {@link Comparable#compareTo(Object)} to conform to the {@link
   * Comparable} interface.</p>
   *
   * <p>This implementation merely loops through the bytes of each encoded condition and returns the
   * result of that comparison.</p>
   *
   * @param that A {@link Condition} to compare against this condition.
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
