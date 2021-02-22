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

  /**
   * Required-args Constructo.
   *
   * @param value An instance of {@link BigInteger}.
   */
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
    return FluentBigInteger.of(
      new BigDecimal(this.value).multiply(new BigDecimal(ratio.numerator()))
        .divide(new BigDecimal(ratio.denominator()), RoundingMode.CEILING).toBigIntegerExact()
    );
  }

  /**
   * Multiply the {@link BigInteger} value by the supplied {@link Ratio}, and then pull the final value to the "floor"
   * so there is no remainder.
   *
   * @param amount A {@link BigDecimal} to multiply by.
   *
   * @return A {@link FluentBigInteger} for further processing.
   */
  public FluentBigInteger timesCeil(final BigDecimal amount) {
    Objects.requireNonNull(amount);
    return FluentBigInteger.of(
      new BigDecimal(getValue()).multiply(amount).setScale(0, RoundingMode.CEILING).toBigIntegerExact()
    );
  }

  /**
   * Divide {@link #getValue()} by {@code divisor} and then return the ceiling.
   *
   * @param divisor A {@link BigDecimal} to divide by.
   *
   * @return A {@link FluentBigInteger} for further processing.
   */
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
   * Compute whether {@link #getValue()} is greater-than 0 (i.e., positive).
   *
   * @return {@code true} if {@link #getValue()} is greater-than 0.
   */
  public boolean isPositive() {
    return FluentCompareTo.is(this.getValue()).greaterThan(BigInteger.ZERO);
  }

  /**
   * Compute whether {@link #getValue()} is not greater-than 0 (i.e., not positive).
   *
   * @return {@code true} if {@link #getValue()} is less-than or equal-to 0.
   */
  public boolean isNotPositive() {
    return !isPositive();
  }

  /**
   * Return the value of this fluent wrapper.
   *
   * @return A {@link BigInteger}.
   */
  public BigInteger getValue() {
    return value;
  }

  /**
   * Choose between {@link #getValue()} and {@link UnsignedLong#MAX_VALUE} and return the smaller value.
   *
   * @return An {@link UnsignedLong} representing the smaller of {@link #getValue()} and {@link UnsignedLong#MAX_VALUE}.
   */
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
}
