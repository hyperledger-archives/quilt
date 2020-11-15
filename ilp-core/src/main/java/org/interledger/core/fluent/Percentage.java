package org.interledger.core.fluent;

import java.math.BigDecimal;
import org.immutables.value.Value.Immutable;

/**
 * A wrapper for a percentage that is always positive. 100% is equal to the number 1.0000 and 1% is equal to the number
 * 0.0100. This implementation allows for a percentage with 2 decimal places, e.g., 32.34%
 */
public interface Percentage extends Comparable<Percentage> {

  Percentage ZERO_PERCENT = of(BigDecimal.ZERO);
  Percentage ONE_PERCENT = of(BigDecimal.valueOf(0.01));
  Percentage FIFTY_PERCENT = of(BigDecimal.valueOf(0.5));
  Percentage ONE_HUNDRED_PERCENT = of(BigDecimal.ONE);

  static Percentage of(final BigDecimal value) {
    return ImmutablePercentage.builder()
      .value(value)
      .build();
  }

  BigDecimal value();

  @Immutable
  abstract class AbstractPercentage implements Percentage {

    @Override
    public String toString() {
      return this.value().movePointRight(2).toString() + "%";
    }

    @Override
    public int compareTo(Percentage o) {
      return this.value().compareTo(o.value());
    }

    @Override
    public abstract BigDecimal value();
  }

}
