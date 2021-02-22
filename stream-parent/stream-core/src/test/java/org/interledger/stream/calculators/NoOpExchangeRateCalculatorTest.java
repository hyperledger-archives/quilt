package org.interledger.stream.calculators;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.stream.Denominations;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

/**
 * Unit tests for {@link NoOpExchangeRateCalculator}.
 */
public class NoOpExchangeRateCalculatorTest {

  @Test
  public void calculateMinAmountToAcceptInReceiverAmountMode() {
    ExchangeRateCalculator calc = new NoOpExchangeRateCalculator();
    assertThat(calc.calculateMinAmountToAccept(UnsignedLong.ONE, Denominations.USD, Denominations.EUR))
      .isEqualTo(UnsignedLong.ZERO);
  }

  @Test
  public void calculateMinAmountToAcceptInSenderAmountMode() {
    ExchangeRateCalculator calc = new NoOpExchangeRateCalculator();
    assertThat(calc.calculateMinAmountToAccept(UnsignedLong.ONE, Denominations.USD))
      .isEqualTo(UnsignedLong.ZERO);
  }
}
