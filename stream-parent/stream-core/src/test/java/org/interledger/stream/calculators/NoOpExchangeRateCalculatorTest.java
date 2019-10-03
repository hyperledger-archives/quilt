package org.interledger.stream.calculators;

import com.google.common.primitives.UnsignedLong;
import org.interledger.stream.Denominations;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NoOpExchangeRateCalculatorTest {

  @Test
  public void calculate() {
    ExchangeRateCalculator calc = new NoOpExchangeRateCalculator();
    assertThat(calc.calculate(UnsignedLong.ONE, Denominations.USD, Denominations.EUR)).isEqualTo(UnsignedLong.ZERO);
  }
}
