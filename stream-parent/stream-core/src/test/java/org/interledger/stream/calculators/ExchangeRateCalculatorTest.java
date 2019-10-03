package org.interledger.stream.calculators;

import com.google.common.primitives.UnsignedLong;
import org.interledger.stream.Denominations;
import org.junit.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

public class ExchangeRateCalculatorTest {

  @Test
  public void scaled() {
    ExchangeRateCalculator calculator = spy(ExchangeRateCalculator.class);
    assertThat(calculator.scaled(UnsignedLong.ONE, Denominations.US_CENTS)).isEqualTo(BigDecimal.valueOf(.01));
  }
}
