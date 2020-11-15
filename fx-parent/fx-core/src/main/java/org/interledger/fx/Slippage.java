package org.interledger.fx;

import org.interledger.core.fluent.FluentCompareTo;
import org.interledger.core.fluent.Percentage;

import com.google.common.base.Preconditions;
import org.immutables.value.Value;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

/**
 * <p>The amount of exchange rate fluctuation, in percentage terms, that a system or operation will tolerate. For
 * example, if the exchange rate for a percentage 0.25, but the accepted slippage is 1%, then the exchange rate `R` is
 * considered valid if it it conforms to the following equation: `0.2475 &lt;= R &lt; 0.2525`.</p>
 *
 * <p>Note that slippage values must be greater-than or equal-to zero, and less-than or equal-to 100, where slippage
 * `S` conforms to this equation: `0 &lt;= S &lt;= 100`.</p>
 */
@Immutable
public interface Slippage extends Comparable<Slippage> {

  Slippage NONE = of(Percentage.ZERO_PERCENT);
  Slippage ONE_PERCENT = of(Percentage.ONE_PERCENT);

  /**
   * Default builder.
   *
   * @param value A {@link Percentage} to build from.
   *
   * @return A {@link Slippage}.
   */
  static Slippage of(final Percentage value) {
    return ImmutableSlippage.builder()
      .value(value)
      .build();
  }

  /**
   * The value of this slippage amount, as a {@link Percentage}.
   *
   * @return The slippage as a {@link Percentage}.
   */
  @Default
  default Percentage value() {
    return Percentage.ZERO_PERCENT;
  }

  @Override
  default int compareTo(final Slippage other) {
    return this.value().compareTo(other.value());
  }

  /**
   * Preconditions check for immutables support.
   */
  @Value.Check
  default void check() {
    Preconditions.checkState(
      FluentCompareTo.is(value()).between(Percentage.ZERO_PERCENT, Percentage.ONE_HUNDRED_PERCENT),
      "Slippage must be a percentage between 0% and 100% (inclusive)"
    );
  }
}
