package org.interledger.core.fluent;

import com.google.common.base.Preconditions;
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
   * @return wrapped value
   */
  public static FluentUnsignedLong of(final UnsignedLong value) {
    return new FluentUnsignedLong(value);
  }

  private final UnsignedLong value;

  private FluentUnsignedLong(final UnsignedLong value) {
    this.value = Objects.requireNonNull(value);
  }

  /**
   * Multiply the {@link UnsignedLong} value by the supplied {@link Ratio}, and then pull the final value to the "floor"
   * so there is no remainder.
   *
   * @param ratio A {@link Ratio} to multiply by.
   * @return A {@link FluentUnsignedLong} for further processing.
   */
  public FluentUnsignedLong timesFloor(final Ratio ratio) {
    Objects.requireNonNull(ratio);

    // TODO: Unit test: numbers bigger than unsigned-long.

    return FluentUnsignedLong.of(
      UnsignedLong.valueOf(this.value.bigIntegerValue().multiply(ratio.numerator().bigIntegerValue())
        .divide(ratio.denominator().bigIntegerValue()))
    );
  }

  /**
   * Multiply the {@link UnsignedLong} value by the supplied {@link Ratio}, and then pull the final value to the "floor"
   * so there is no remainder.
   *
   * @param ratio A {@link Ratio} to multiply by.
   * @return A {@link FluentUnsignedLong} for further processing.
   */
  public FluentUnsignedLong timesCeil(final Ratio ratio) {
    Objects.requireNonNull(ratio);

    // TODO: Unit test: numbers bigger than unsigned-long.

    return FluentUnsignedLong
      .of(this.value.times(ratio.numerator()).dividedBy(ratio.denominator()).plus(UnsignedLong.ONE));
  }

  public FluentUnsignedLong divideCeil(final UnsignedLong divisor) {
    Objects.requireNonNull(divisor);
    Preconditions.checkState(FluentUnsignedLong.of(divisor).isPositive(), "divisor must be positive");

    UnsignedLong newValue = FluentUnsignedLong.of(this.getValue().mod(divisor)).isPositive()
      ? this.value.dividedBy(divisor).plus(UnsignedLong.ONE)
      : this.value.dividedBy(divisor);

    return FluentUnsignedLong.of(newValue);
  }

  /**
   * Checks if wrapped value is equal than the given one.
   *
   * @param other given value
   * @return true if wrapped value is equal to given value
   */
  public FluentUnsignedLong orGreater(final UnsignedLong other) {
    Objects.requireNonNull(other);
    if (FluentCompareTo.is(this.value).greaterThanEqualTo(other)) {
      return new FluentUnsignedLong(this.value);
    } else {
      return new FluentUnsignedLong(other);
    }
  }

  // TODO: Javadoc + Unit tests.
  public FluentUnsignedLong orLesser(final UnsignedLong other) {
    Objects.requireNonNull(other);
    if (FluentCompareTo.is(this.value).lessThanOrEqualTo(other)) {
      return new FluentUnsignedLong(this.value);
    } else {
      return new FluentUnsignedLong(other);
    }
  }

  public boolean isPositive() {
    return FluentCompareTo.is(this.getValue()).greaterThan(UnsignedLong.ZERO);
  }

  public UnsignedLong getValue() {
    return value;
  }
}
