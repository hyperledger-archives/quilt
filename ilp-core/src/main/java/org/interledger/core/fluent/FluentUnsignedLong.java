package org.interledger.core.fluent;

import com.google.common.primitives.UnsignedLong;

import java.util.Objects;

/**
 * Utilities for operating on {@link UnsignedLong}.
 */
public class FluentUnsignedLong {

  /**
   * Create a {@link FluentCompareTo} for the given value.
   *
   * @param value An {@linkn UnsignedLong} value to wrap
   *
   * @return wrapped value
   */
  public static FluentUnsignedLong of(final UnsignedLong value) {
    return new FluentUnsignedLong(value);
  }

  private final UnsignedLong value;

  /**
   * Required-args Constructor.
   *
   * @param value A {@link UnsignedLong}.
   */
  private FluentUnsignedLong(final UnsignedLong value) {
    this.value = Objects.requireNonNull(value);
  }

  /**
   * Multiply the {@link UnsignedLong} value by the supplied {@link Ratio}, and then pull the final value to the "floor"
   * so there is no remainder.
   *
   * @param ratio A {@link Ratio} to multiply by.
   *
   * @return A {@link FluentUnsignedLong} for further processing.
   *
   * @throws IllegalStateException if the final value exceeds {@link UnsignedLong#MAX_VALUE}.
   */
  public FluentUnsignedLong timesFloorOrZero(final Ratio ratio) {
    Objects.requireNonNull(ratio);

    return FluentUnsignedLong.of(
      ratio.multiplyFloorOrZero(this.getValue())
    );
  }

  /**
   * Multiply the {@link UnsignedLong} value by the supplied {@link Ratio}, and then pull the final value to the "floor"
   * so there is no remainder. If an overflow condition occurs, then return {@link UnsignedLong#ZERO}.
   *
   * @param ratio A {@link Ratio} to multiply by.
   *
   * @return A {@link FluentUnsignedLong} with the new value for further processing.
   *
   * @throws IllegalStateException if the final value exceeds {@link UnsignedLong#MAX_VALUE}.
   */
  public FluentUnsignedLong timesCeilOrZero(final Ratio ratio) {
    Objects.requireNonNull(ratio);

    return FluentUnsignedLong.of(
      ratio.multiplyCeilOrZero(this.getValue())
    );
  }

  private static final UnsignedLong TWO = UnsignedLong.valueOf(2L);

  /**
   * Divide {@link #getValue()} by 2.
   *
   * @return A {@link FluentUnsignedLong} with the new value for further processing.
   */
  public FluentUnsignedLong halfCeil() {
    return FluentUnsignedLong.of(
      FluentUnsignedLong.of(this.getValue().mod(TWO)).isPositive()
        ? this.value.dividedBy(TWO).plus(UnsignedLong.ONE)
        : this.value.dividedBy(TWO)
    );
  }

  /**
   * Assign {@link #value} to the greater of {@link #getValue()} or {@code other}.
   *
   * @param other given value
   *
   * @return A new {@link FluentUnsignedLong} with the greater of {@link #getValue()} or {@code other}.
   */
  public FluentUnsignedLong orGreater(final UnsignedLong other) {
    Objects.requireNonNull(other);
    if (FluentCompareTo.is(this.value).greaterThanEqualTo(other)) {
      return new FluentUnsignedLong(this.value);
    } else {
      return new FluentUnsignedLong(other);
    }
  }

  /**
   * Assign {@link #value} to the lesser of {@link #getValue()} or {@code other}.
   *
   * @param other given value
   *
   * @return A new {@link FluentUnsignedLong} with the lesser of {@link #getValue()} or {@code other}.
   */
  public FluentUnsignedLong orLesser(final UnsignedLong other) {
    Objects.requireNonNull(other);
    if (FluentCompareTo.is(this.value).lessThanOrEqualTo(other)) {
      return new FluentUnsignedLong(this.value);
    } else {
      return new FluentUnsignedLong(other);
    }
  }

  /**
   * Determine if {@link #getValue()} is greater-than 0.
   *
   * @return {@code true} if {@link #getValue()} is positive (i.e., greater-than zero); {@code false otherwise}.
   */
  public boolean isPositive() {
    return FluentCompareTo.is(this.getValue()).greaterThan(UnsignedLong.ZERO);
  }

  /**
   * Determine if {@link #getValue()} is not greater-than 0.
   *
   * @return {@code true} if {@link #getValue()} is not positive (i.e., not greater-than zero); {@code false otherwise}.
   */
  public boolean isNotPositive() {
    return !this.isPositive();
  }

  /**
   * Get the value of this fluent wrapper.
   *
   * @return An {@link UnsignedLong}.
   */
  public UnsignedLong getValue() {
    return value;
  }

  /**
   * Subtract {@code amount} from {@link #getValue()} and return the value. If the value would be negative, return
   * {@link UnsignedLong#ZERO}.
   *
   * @param amount An {@link UnsignedLong} to subtract from this value.
   *
   * @return A {@link FluentUnsignedLong} with the new value for further processing.
   */
  public FluentUnsignedLong minusOrZero(final UnsignedLong amount) {
    Objects.requireNonNull(amount);
    if (FluentCompareTo.is(this.getValue()).greaterThanEqualTo(amount)) {
      return FluentUnsignedLong.of(this.getValue().minus(amount));
    } else {
      return FluentUnsignedLong.of(UnsignedLong.ZERO);
    }
  }
}
