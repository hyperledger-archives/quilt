package org.interledger.fx;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.primitives.UnsignedLong;
import java.math.BigDecimal;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link ExchangeRateCalculator}.
 */
public class ExchangeRateCalculatorTest {

  private ExchangeRateCalculator calculator;

  @Before
  public void setUp() {
    calculator = new ExchangeRateCalculator() {
    };
  }

  @Test
  public void scaled() {
    assertThat(calculator.scaled(UnsignedLong.ONE, Denominations.USD)).isEqualTo(BigDecimal.ONE);
    assertThat(calculator.scaled(UnsignedLong.ONE, Denominations.USD_CENTS)).isEqualTo(BigDecimal.valueOf(.01));
    assertThat(calculator.scaled(UnsignedLong.ONE, Denominations.EUR)).isEqualTo(BigDecimal.ONE);
    assertThat(calculator.scaled(UnsignedLong.ONE, Denominations.EUR_CENTS)).isEqualTo(BigDecimal.valueOf(.01));
    assertThat(calculator.scaled(UnsignedLong.ONE, Denominations.XRP_DROPS)).isEqualTo(new BigDecimal(".000001"));
    assertThat(calculator.scaled(UnsignedLong.ONE, Denominations.XRP_MILLI_DROPS))
      .isEqualTo(new BigDecimal(".000000001"));
    assertThat(
      calculator.scaled(UnsignedLong.ONE, Denomination.builder().assetCode("USD").assetScale((short) 0).build())
    ).isEqualTo(BigDecimal.valueOf(1));
  }
}
