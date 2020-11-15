package org.interledger.fx;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit tests for {@link ScaledExchangeRate}.
 */
public class ScaledExchangeRateTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testHappyPath() {
    ScaledExchangeRate scaledExchangeRate = ScaledExchangeRate.builder()
      .value(BigDecimal.TEN)
      .slippage(Slippage.ONE_PERCENT)
      .inputScale((short) 9)
      .build();

    assertThat(scaledExchangeRate.value()).isEqualTo(BigInteger.TEN);
    assertThat(scaledExchangeRate.slippage()).isEqualTo(Slippage.ONE_PERCENT);
    assertThat(scaledExchangeRate.inputScale()).isEqualTo((short) 9);
  }

  @Test
  public void testBuildWithZero() {
    ScaledExchangeRate scaledExchangeRate = ScaledExchangeRate.builder()
      .value(BigDecimal.ZERO)
      .inputScale((short) 2)
      .build();
  }

  @Test
  public void testEmptyBuild() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(
      "Cannot build ScaledExchangeRate, some of required attributes are not set [value, inputScale]");

    ScaledExchangeRate.builder().build();
  }

  @Test
  public void testToString() {
    ScaledExchangeRate scaledExchangeRate = ScaledExchangeRate.builder()
      .value(BigDecimal.ONE)
      .inputScale((short) 2)
      .build();

    assertThat(scaledExchangeRate.toString())
      .isEqualTo("ScaledExchangeRate{value=1, inputScale=2, slippage=Slippage{value=0.00%}}");
  }

  @Test
  public void testCompareTo() {
    ScaledExchangeRate scaledExchangeRate0 = ScaledExchangeRate.builder()
      .value(BigDecimal.ZERO)
      .inputScale((short) 2)
      .build();
    ScaledExchangeRate scaledExchangeRate1 = ScaledExchangeRate.builder()
      .value(BigDecimal.ONE)
      .inputScale((short) 2)
      .build();

    assertThat(scaledExchangeRate0.compareTo(scaledExchangeRate0)).isEqualTo(0);
    assertThat(scaledExchangeRate0.compareTo(scaledExchangeRate1)).isEqualTo(-1);
    assertThat(scaledExchangeRate1.compareTo(scaledExchangeRate0)).isEqualTo(1);
  }

  @Test
  public void testEquals() {
    ScaledExchangeRate scaledExchangeRate0 = ScaledExchangeRate.builder()
      .value(BigDecimal.ZERO)
      .inputScale((short) 2)
      .build();
    ScaledExchangeRate scaledExchangeRate1 = ScaledExchangeRate.builder()
      .value(BigDecimal.ONE)
      .inputScale((short) 2)
      .build();

    assertThat(scaledExchangeRate0.equals(scaledExchangeRate0)).isTrue();
    assertThat(scaledExchangeRate0.equals(scaledExchangeRate1)).isFalse();
    assertThat(scaledExchangeRate1.equals(scaledExchangeRate0)).isFalse();
  }


}