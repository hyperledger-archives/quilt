package org.interledger.cryptoconditions;

import java.util.EnumSet;
import java.util.Objects;

/**
 * An abstract implementation of {@link CompoundCondition} that extends {@link Sha256Condition} to
 * provide common functionality for all compound condition classes.
 */
public abstract class CompoundSha256Condition extends Sha256Condition implements CompoundCondition {

  private final EnumSet<CryptoConditionType> subtypes;

  /**
   * Constructor that accepts a fingerprint and a cost number.
   *
   * @param type        A {@link CryptoConditionType} that represents the type of this condition.
   * @param cost        A {@link long} representing the anticipated cost of this condition,
   *                    calculated per
   *                    the rules of the crypto-conditions specification.
   * @param fingerprint The binary representation of the fingerprint for this condition.
   * @param subtypes    An {@link EnumSet} of types that this condition will hold as subconditions.
   */
  protected CompoundSha256Condition(
      final CryptoConditionType type, final long cost, final byte[] fingerprint,
      final EnumSet<CryptoConditionType> subtypes
  ) {
    super(type, cost, fingerprint);
    this.subtypes = EnumSet.copyOf(Objects.requireNonNull(subtypes));
  }

  @Override
  public final EnumSet<CryptoConditionType> getSubtypes() {
    return EnumSet.copyOf(subtypes);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }
    if (!super.equals(object)) {
      return false;
    }

    CompoundSha256Condition that = (CompoundSha256Condition) object;

    return subtypes.equals(that.subtypes);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + subtypes.hashCode();
    return result;
  }
}
