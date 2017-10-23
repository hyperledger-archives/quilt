package org.interledger.cryptoconditions;

import java.util.Objects;

/**
 * This class provides shared, concrete logic for all conditions.
 */
public abstract class FulfillmentBase<C extends Condition> implements Fulfillment<C> {

  private final CryptoConditionType type;

  /**
   * Default internal constructor for all fulfillments.
   *
   * @param type The type of this condition.
   */
  protected FulfillmentBase(final CryptoConditionType type) {
    this.type = Objects.requireNonNull(type);
  }

  @Override
  public final CryptoConditionType getType() {
    return this.type;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }

    FulfillmentBase<?> that = (FulfillmentBase<?>) object;

    return type == that.type;
  }

  @Override
  public int hashCode() {
    return type.hashCode();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("FulfillmentBase{");
    sb.append("type=").append(type);
    sb.append('}');
    return sb.toString();
  }
}
