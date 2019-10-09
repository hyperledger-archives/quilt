package org.interledger.stream.calculators;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.stream.Denominations;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

import java.util.Optional;

public class NoOpExchangeRateCalculatorTest {

  @Test
  public void calculate() {
    ExchangeRateCalculator calc = new NoOpExchangeRateCalculator();
    assertThat(calc.calculateMinAmountToAccept(UnsignedLong.ONE, Denominations.USD, Optional.of(Denominations.EUR)))
        .isEqualTo(UnsignedLong.ZERO);
  }
}
