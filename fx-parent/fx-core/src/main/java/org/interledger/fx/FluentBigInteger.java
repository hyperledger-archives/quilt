package org.interledger.fx;

import com.google.common.base.Preconditions;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Objects;
import org.interledger.core.fluent.FluentCompareTo;
import org.interledger.core.fluent.Ratio;

/**
 * Utilities for operating on {@link BigInteger}.
 */
public class FluentBigInteger {

  /**
   * Create a {@link FluentCompareTo} for the given value.
   *
   * @param value An {@linkn BigInteger} value to wrap
   * @return wrapped value
   */
  public static FluentBigInteger of(final BigInteger value) {
    return new FluentBigInteger(value);
  }

  private final BigInteger value;

  private FluentBigInteger(final BigInteger value) {
    this.value = Objects.requireNonNull(value);
  }

  // TODO: Javadoc

  /**
   * Multiply the {@link BigInteger} value by the supplied {@link Ratio}, and then pull the final value to the "floor"
   * so there is no remainder.
   *
   * @param ratio A {@link Ratio} to multiply by.
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
   * @return A {@link FluentBigInteger} for further processing.
   */
  public FluentBigInteger timesCeil(final BigDecimal percentage) {
    Objects.requireNonNull(percentage);

    // TODO: Unit test: numbers bigger than unsigned-long.

    return FluentBigInteger.of(
      new BigDecimal(getValue()).multiply(percentage).setScale(0, RoundingMode.CEILING).toBigIntegerExact()
    );
  }

  /**
   * Multiply the {@link BigInteger} value by the supplied {@link Ratio}, and then pull the final value to the "floor"
   * so there is no remainder.
   *
   * @param ratio A {@link Ratio} to multiply by.
   * @return A {@link FluentBigInteger} for further processing.
   */
  public FluentBigInteger timesCeil(final ScaledExchangeRate scaledExchangeRate) {
    Objects.requireNonNull(scaledExchangeRate);

    // TODO: Unit test: numbers bigger than unsigned-long.

    return FluentBigInteger.of(
      new BigDecimal(getValue()).multiply(scaledExchangeRate.value())
        .setScale(0, RoundingMode.CEILING)
        .toBigIntegerExact()
    );
  }

  public FluentBigInteger divideCeil(final BigInteger divisor) {
    Objects.requireNonNull(divisor);
    Preconditions.checkState(FluentBigInteger.of(divisor).isPositive(), "divisor must be positive");

    return FluentBigInteger.of(
      new BigDecimal(this.getValue()).divide(new BigDecimal(divisor), RoundingMode.CEILING).toBigIntegerExact()
    );
  }

  /**
   * Checks if wrapped value is equal than the given one.
   *
   * @param other given value
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

  public BigInteger getValue() {
    return value;
  }
}
