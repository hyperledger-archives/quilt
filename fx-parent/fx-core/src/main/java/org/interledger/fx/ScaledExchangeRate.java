package org.interledger.fx;

import org.interledger.core.fluent.FluentCompareTo;
import org.interledger.core.fluent.Ratio;
import org.interledger.fx.ImmutableScaledExchangeRate.Builder;

import com.google.common.base.Preconditions;
import org.immutables.value.Value;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A wrapper for a scaled exchange rate that accounts for slippage. For more information about the meaning of `scale`,
 * see {@link Denomination#assetScale}.
 */
@Immutable
public interface ScaledExchangeRate extends Comparable<ScaledExchangeRate> {

  static Builder builder() {
    return ImmutableScaledExchangeRate.builder();
  }

  /**
   * The value of this exchange rate.
   *
   * @return A {@link Ratio} representing the value of this scaled exchange rate.
   */
  Ratio value();

  /**
   * The scale used to compute this scaled exchange rate. For example, if a scaled exchange rate has a `value` of 100,
   * an original source scale of 2, and an original destination scale of `4`, then the original FX rate is 100.
   * Conversely, if a scaled exchange rate has a `value` of 100, an original source scale of `2`, and an original
   * destination scale of `0`, then the original FX rate is 0.01.
   *
   * @return A short representing the original source asset scale used to assemble this instance.
   */
  short originalSourceScale();

  /**
   * The scale used to compute this scaled exchange rate. For example, if a scaled exchange rate has a value of 100.0,
   * and a scale of 2, then the actual rate is 1.0.
   *
   * @return A short representing the original destination asset scale used to assemble this instance.
   */
  short originalDestinationScale();

  /**
   * The amount of slippage that this scaled exchange rate will tolerate. Used to compute the upper and lower-bound
   * rates (default is 1% or 0.01).
   *
   * @return A {@link Slippage}.
   */
  @Default
  default Slippage slippage() {
    return Slippage.ONE_PERCENT;
  }

  /**
   * The smallest value of this scaled rate when allowing for slippage.
   *
   * @return A {@link Ratio} representing the lower-bound of this scaled FX rate.
   */
  default Ratio lowerBound() {
    // value * (100% - slippage%) --> 50 * (1.0 - .01)
    return Ratio.from(this.value().toBigDecimal().multiply(BigDecimal.ONE.subtract(slippage().value().value())));
  }

  /**
   * The largest value of this scaled rate when allowing for slippage.
   *
   * @return A {@link Ratio} representing the upper-bound of this scaled FX rate.
   */
  default Ratio upperBound() {
    // value * (100% + slippage%) --> 50 * (1.0 + .01)
    return Ratio.from(this.value().toBigDecimal().multiply(BigDecimal.ONE.add(slippage().value().value())));
  }

  @Override
  default int compareTo(final ScaledExchangeRate othere) {
    Objects.requireNonNull(othere);
    return this.value().compareTo(othere.value());
  }

  /**
   * Preconditions check for immutables.
   */
  @Value.Check
  default void check() {
    Preconditions.checkState(
      FluentCompareTo.is(value()).greaterThanEqualTo(Ratio.ZERO),
      "ScaledExchangeRate must be greater-than or equal-to 0."
    );
  }
}
