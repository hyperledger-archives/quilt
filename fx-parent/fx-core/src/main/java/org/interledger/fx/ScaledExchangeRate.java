package org.interledger.fx;

import com.google.common.base.Preconditions;
import java.math.BigDecimal;
import java.math.MathContext;
import javax.money.convert.ExchangeRate;
import org.immutables.value.Value;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Lazy;
import org.interledger.core.fluent.FluentCompareTo;
import org.interledger.fx.ImmutableScaledExchangeRate.Builder;

/**
 * A wrapper for a scaled exchange rate that accounts for slippage.
 * <p>
 * For more information about the meaning of `scale`, see {@link Denomination#assetScale}.
 */
@Immutable
public interface ScaledExchangeRate extends Comparable<ScaledExchangeRate> {

  static Builder builder() {
    return ImmutableScaledExchangeRate.builder();
  }

  /**
   * The value of this exchange rate (corresponds to {@link ExchangeRate#getFactor()}).
   *
   * @return
   */
  BigDecimal value();

  /**
   * The scale used to compute this scaled exchange rate. For example, if a scaled exchange rate has a value of 100.0,
   * and a scale of 2, then the actual rate is 1.0.
   *
   * @return
   */
  short inputScale();

  /**
   * The amount of slippage that this scaled exchange rate will tolerate. Used to compute the upper and lower-bound
   * rates (default is 0).
   *
   * @return A {@link Slippage}.
   */
  @Default
  default Slippage slippage() {
    return Slippage.NONE;
  }

  /**
   * The smallest value of this scaled rate when allowing for slippage.
   *
   * @return
   */
  default BigDecimal lowerBound() {
    // TODO: Unit tests.
    // value * (100% - slippage%) --> 50 * (1.0 - .01)
    return this.value().multiply(BigDecimal.ONE.subtract(slippage().value().value()));
  }

  /**
   * The largest value of this scaled rate when allowing for slippage.
   *
   * @return
   */
  default BigDecimal upperBound() {
    // TODO: Unit tests.
    // value * (100% + slippage%) --> 50 * (1.0 + .01)
    return this.value().multiply(BigDecimal.ONE.add(slippage().value().value()));
  }

  @Override
  default int compareTo(ScaledExchangeRate o) {
    return this.value().compareTo(o.value());
  }

//  /**
//   * Multiply a number by a ratio and return the "floor" value.
//   *
//   * @param amount
//   * @return
//   */
//  // TODO: Unit test to validate floor.
//  @Derived
//  default ScaledExchangeRate timesFloor(final UnsignedLong amount) {
//    Objects.requireNonNull(amount);
//    return amount.times(this.numerator()).dividedBy(this.denominator());
//  }
//
//  // TODO: Unit test
//  @Derived
//  default UnsignedLong timesCeil(UnsignedLong amount) {
//    boolean isPositive = FluentCompareTo.is(modulo(amount, this)).greaterThan(UnsignedLong.ZERO);
//    if (isPositive) {
//      return (amount.times(this.numerator()).dividedBy(this.denominator())).plus(UnsignedLong.ONE);
//    } else {
//      return amount.times(this.numerator()).dividedBy(this.denominator());
//    }
//  }

  /**
   * Returns the reciprocal of this scaled exchange rate.
   *
   * @return A {@link ScaledExchangeRate}.
   */
  @Lazy
  default BigDecimal reciprocal() {
    // If this rate is zero, then the reciprocal should also be zero in order to avoid
    // a "divide by zero" ArithmeticException
    if (this.value().equals(BigDecimal.ZERO)) {
      return BigDecimal.ZERO;
    } else {
      // TODO: Unit tests.
      return BigDecimal.ONE.divide(this.value(), MathContext.DECIMAL64);
    }
  }

  @Value.Check
  default void check() {
    Preconditions.checkState(
      FluentCompareTo.is(value()).greaterThanEqualTo(BigDecimal.ZERO),
      "ScaledExchangeRate must be greater-than or equal-to 0."
    );
  }
}
