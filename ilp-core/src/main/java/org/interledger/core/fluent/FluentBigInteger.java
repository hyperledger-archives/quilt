package org.interledger.core.fluent;

import com.google.common.primitives.UnsignedLong;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Utilities for operating on {@link BigInteger}.
 */
public class FluentBigInteger {

  private static final BigInteger UNSIGNED_LONG_MAX = UnsignedLong.MAX_VALUE.bigIntegerValue();

  /**
   * Create a {@link FluentCompareTo} for the given value.
   *
   * @param value An {@linkn BigInteger} value to wrap
   *
   * @return wrapped value
   */
  public static FluentBigInteger of(final BigInteger value) {
    return new FluentBigInteger(value);
  }

  private final BigInteger value;

  private FluentBigInteger(final BigInteger value) {
    this.value = Objects.requireNonNull(value);
  }

  /**
   * Multiply the {@link BigInteger} value by the supplied {@link Ratio}, and then pull the final value to the "floor"
   * so there is no remainder.
   *
   * @param ratio A {@link Ratio} to multiply by.
   *
   * @return A {@link FluentBigInteger} for further processing.
   */
  public FluentBigInteger timesFloor(final Ratio ratio) {
    Objects.requireNonNull(ratio);

    // TODO: Unit test: numbers bigger than unsigned-long.

    return FluentBigInteger.of(
      new BigDecimal(this.value).multiply(new BigDecimal(ratio.numerator()))
        .divide(new BigDecimal(ratio.denominator()), RoundingMode.FLOOR).toBigIntegerExact()
    );
  }

  /**
   * Multiply the {@link BigInteger} value by the supplied {@link Ratio}, and then pull the final value to the "floor"
   * so there is no remainder.
   *
   * @param ratio A {@link Ratio} to multiply by.
   *
   * @return A {@link FluentBigInteger} for further processing.
   */
  public FluentBigInteger timesCeil(final Ratio ratio) {
    Objects.requireNonNull(ratio);

    // TODO: Unit test: numbers bigger than unsigned-long.

    return FluentBigInteger.of(
      new BigDecimal(this.value).multiply(new BigDecimal(ratio.numerator()))
        .divide(new BigDecimal(ratio.denominator()), RoundingMode.CEILING).toBigIntegerExact()
    );
  }

  // TODO: Unit test to validate this is correct.
  public FluentBigInteger divideCeil(final BigInteger divisor) {
    Objects.requireNonNull(divisor);

    if (BigInteger.ZERO.equals(divisor)) {
      return FluentBigInteger.of(BigInteger.ZERO);
    }

    BigInteger newValue = FluentBigInteger.of(this.getValue().mod(divisor)).isPositive()
      ? this.value.divide(divisor).add(BigInteger.ONE)
      : this.value.divide(divisor);

    return FluentBigInteger.of(newValue);
  }

  /**
   * Checks if wrapped value is equal than the given one.
   *
   * @param other given value
   *
   * @return true if wrapped value is equal to given value
   */
  public FluentBigInteger orGreater(final BigInteger other) {
    Objects.requireNonNull(other);
    if (FluentCompareTo.is(this.value).greaterThanEqualTo(other)) {
      return new FluentBigInteger(this.value);
    } else {
      return new FluentBigInteger(other);
    }
  }

  // TODO: Javadoc + Unit tests.
  public FluentBigInteger orLesser(final BigInteger other) {
    Objects.requireNonNull(other);
    if (FluentCompareTo.is(this.value).lessThanOrEqualTo(other)) {
      return new FluentBigInteger(this.value);
    } else {
      return new FluentBigInteger(other);
    }
  }

  public boolean isPositive() {
    return FluentCompareTo.is(this.getValue()).greaterThan(BigInteger.ZERO);
  }

  public boolean isNotPositive() {
    return !isPositive();
  }

  public BigInteger getValue() {
    return value;
  }

  /**
   * Multiply the {@link BigInteger} value by the supplied {@link Ratio}, and then pull the final value to the "floor"
   * so there is no remainder.
   *
   * @param ratio A {@link Ratio} to multiply by.
   *
   * @return A {@link FluentBigInteger} for further processing.
   */
  public FluentBigInteger timesFloor(final Percentage percentage) {
    Objects.requireNonNull(percentage);

    // TODO: Unit test: numbers bigger than unsigned-long.

    return FluentBigInteger.of(
      new BigDecimal(getValue()).multiply(percentage.value()).setScale(0, RoundingMode.FLOOR).toBigIntegerExact()
    );

//    return FluentBigInteger.of(
//      BigInteger.valueOf(this.value.multiply(ratio.numerator().bigIntegerValue())
//        .divide(ratio.denominator().bigIntegerValue()))
//    );
  }

  /**
   * Multiply the {@link BigInteger} value by the supplied {@link Ratio}, and then pull the final value to the "floor"
   * so there is no remainder.
   *
   * @param ratio A {@link Ratio} to multiply by.
   *
   * @return A {@link FluentBigInteger} for further processing.
   */
  public FluentBigInteger timesCeil(final Percentage percentage) {
    Objects.requireNonNull(percentage);

    return timesCeil(percentage.value());
  }

  /**
   * Multiply the {@link BigInteger} value by the supplied {@link Ratio}, and then pull the final value to the "floor"
   * so there is no remainder.
   *
   * @param ratio A {@link Ratio} to multiply by.
   *
   * @return A {@link FluentBigInteger} for further processing.
   */
  public FluentBigInteger timesCeil(final BigDecimal percentage) {
    Objects.requireNonNull(percentage);

    // TODO: Unit test: numbers bigger than unsigned-long.

    return FluentBigInteger.of(
      new BigDecimal(getValue()).multiply(percentage).setScale(0, RoundingMode.CEILING).toBigIntegerExact()
    );
  }

  public UnsignedLong orMaxUnsignedLong() {
    if (FluentCompareTo.is(this.getValue()).greaterThan(UNSIGNED_LONG_MAX)) {
      return UnsignedLong.MAX_VALUE;
    } else {
      return UnsignedLong.valueOf(this.getValue());
    }
  }

  /**
   * Subtract {@code amount} from this {@link BigInteger} and return the result if it's non-negative; otherwise, return
   * {@link BigInteger#ZERO}.
   *
   * @param amount A {@link BigInteger} to subract.
   *
   * @return The difference between this value and {@code amount} if non-negative; otherwise return {@link
   *   BigInteger#ZERO}.
   */
  public FluentBigInteger minusOrZero(final BigInteger amount) {
    Objects.requireNonNull(amount);
    if (FluentCompareTo.is(this.getValue()).greaterThanEqualTo(amount)) {
      return FluentBigInteger.of(this.getValue().subtract(amount));
    } else {
      return FluentBigInteger.of(BigInteger.ZERO);
    }
  }

//  /**
//   * Multiply the {@link BigInteger} value by the supplied {@link Ratio}, and then pull the final value to the "floor"
//   * so there is no remainder.
//   *
//   * @param ratio A {@link Ratio} to multiply by.
//   * @return A {@link FluentBigInteger} for further processing.
//   */
//  public FluentBigInteger timesCeil(final ScaledExchangeRate scaledExchangeRate) {
//    Objects.requireNonNull(scaledExchangeRate);
//
//    // TODO: Unit test: numbers bigger than unsigned-long.
//
//    return FluentBigInteger.of(
//      new BigDecimal(getValue()).multiply(scaledExchangeRate.value())
//        .setScale(0, RoundingMode.CEILING)
//        .toBigIntegerExact()
//    );
//  }
}
