package org.interledger.core;

import org.immutables.value.Value;

/**
 * A class helper for creating type-safe wrappers using Immutables in conjunction with {@link Wrapped}.
 *
 * @see "http://immutables.github.io/immutable.html#wrapper-types"
 */
public abstract class Wrapper<T extends Comparable<T>> implements Comparable<Wrapper<T>> {

  @Value.Parameter
  public abstract T value();

  @Override
  public int compareTo(Wrapper<T> otherWrapped) {
    return value().compareTo(otherWrapped.value());
  }

  @Override
  public boolean equals(Object obj) {

    if (obj != null && obj instanceof Wrapper) {
      Object otherValue = ((Wrapper) obj).value();
      if (otherValue != null) {
        return otherValue.equals(value());
      }
    }
    return false;
  }

  @Override
  public int hashCode() {
    return value().hashCode();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(" + value() + ")";
  }
}
