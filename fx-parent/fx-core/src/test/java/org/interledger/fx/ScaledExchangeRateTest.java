package org.interledger.fx;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.core.fluent.Percentage;
import org.interledger.core.fluent.Ratio;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Unit tests for {@link ScaledExchangeRate}.
 */
public class ScaledExchangeRateTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testHappyPath() {
    ScaledExchangeRate scaledExchangeRate = ScaledExchangeRate.builder()
      .value(Ratio.from(BigDecimal.TEN))
      .slippage(Slippage.ONE_PERCENT)
      .originalInputScale((short) 9)
      .build();

    assertThat(scaledExchangeRate.value()).isEqualTo(BigDecimal.TEN);
    assertThat(scaledExchangeRate.slippage()).isEqualTo(Slippage.ONE_PERCENT);
    assertThat(scaledExchangeRate.originalInputScale()).isEqualTo((short) 9);
  }

  @Test
  public void testBuildWithZero() {
    ScaledExchangeRate scaledExchangeRate = ScaledExchangeRate.builder()
      .value(Ratio.from(BigDecimal.ZERO))
      .originalInputScale((short) 2)
      .build();
  }

  @Test
  public void testEmptyBuild() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(
      "Cannot build ScaledExchangeRate, some of required attributes are not set [value, originalInputScale]");

    ScaledExchangeRate.builder().build();
  }

  @Test
  public void testToString() {
    ScaledExchangeRate scaledExchangeRate = ScaledExchangeRate.builder()
      .value(Ratio.from(BigDecimal.ONE))
      .originalInputScale((short) 2)
      .build();

    assertThat(scaledExchangeRate.toString())
      .isEqualTo("ScaledExchangeRate{value=1, originalInputScale=2, slippage=Slippage{value=0%}}");
  }

  @Test
  public void testCompareTo() {
    ScaledExchangeRate scaledExchangeRate0 = ScaledExchangeRate.builder()
      .value(Ratio.from(BigDecimal.ZERO))
      .originalInputScale((short) 2)
      .build();
    ScaledExchangeRate scaledExchangeRate1 = ScaledExchangeRate.builder()
      .value(Ratio.from(BigDecimal.ONE))
      .originalInputScale((short) 2)
      .build();

    assertThat(scaledExchangeRate0.compareTo(scaledExchangeRate0)).isEqualTo(0);
    assertThat(scaledExchangeRate0.compareTo(scaledExchangeRate1)).isEqualTo(-1);
    assertThat(scaledExchangeRate1.compareTo(scaledExchangeRate0)).isEqualTo(1);
  }

  @Test
  public void testEquals() {
    ScaledExchangeRate scaledExchangeRate0 = ScaledExchangeRate.builder()
      .value(Ratio.from(BigDecimal.ZERO))
      .originalInputScale((short) 2)
      .build();
    ScaledExchangeRate scaledExchangeRate1 = ScaledExchangeRate.builder()
      .value(Ratio.from(BigDecimal.ONE))
      .originalInputScale((short) 2)
      .build();

    assertThat(scaledExchangeRate0.equals(scaledExchangeRate0)).isTrue();
    assertThat(scaledExchangeRate0.equals(scaledExchangeRate1)).isFalse();
    assertThat(scaledExchangeRate1.equals(scaledExchangeRate0)).isFalse();
  }

  @Test
  public void testBounds() {
    ScaledExchangeRate scaledExchangeRate = ScaledExchangeRate.builder()
      .value(Ratio.from(new BigDecimal("86.84991150442478")))
      .originalInputScale((short) 0)
      .slippage(Slippage.of(Percentage.of(new BigDecimal("0.00051"))))
      .build();

    assertThat(scaledExchangeRate.slippage().value().value()).isEqualTo(new BigDecimal("0.00051"));

    // Lower Bound / MinRate from JS
    BigDecimal minimumRate = BigDecimal.ONE.subtract(scaledExchangeRate.slippage().value().value())
      .multiply(scaledExchangeRate.value().toBigDecimal());
    assertThat(minimumRate).isEqualTo(new BigDecimal("86.8056180495575233622"));

    // LowerBound
    final BigDecimal lowerBoundFromJS = new BigDecimal("86.8056180495575233622");
    assertThat(scaledExchangeRate.lowerBound()).isEqualTo(lowerBoundFromJS);
    assertThat(Ratio.builder()
      .numerator(BigInteger.valueOf(8680561804955754L))
      .denominator(BigInteger.valueOf(100000000000000L))
      .build().toBigDecimal())
      .isEqualTo(new BigDecimal("86.80561804955754"));
    assertThat(scaledExchangeRate.lowerBound()).isEqualTo(lowerBoundFromJS);

    // Value
    assertThat(scaledExchangeRate.value()).isEqualTo(new BigDecimal("86.84991150442478"));

    // Upper Bound
    assertThat(scaledExchangeRate.upperBound()).isEqualTo(new BigDecimal("86.8942049592920366378"));
  }

}