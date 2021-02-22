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
  public void testWhereSourceGreaterThanDest() {
    ScaledExchangeRate scaledExchangeRate = ScaledExchangeRate.builder()
      .value(Ratio.from(BigDecimal.TEN))
      .slippage(Slippage.ONE_PERCENT)
      .originalSourceScale((short) 9)
      .originalDestinationScale((short) 6)
      .build();

    assertThat(scaledExchangeRate.value())
      .isEqualTo(Ratio.builder().numerator(BigInteger.valueOf(100L)).denominator(BigInteger.TEN).build());
    assertThat(scaledExchangeRate.slippage()).isEqualTo(Slippage.ONE_PERCENT);
    assertThat(scaledExchangeRate.originalSourceScale()).isEqualTo((short) 9);
  }

  @Test
  public void testWhereSourceLessThanDest() {
    ScaledExchangeRate scaledExchangeRate = ScaledExchangeRate.builder()
      .value(Ratio.from(BigDecimal.TEN))
      .slippage(Slippage.ONE_PERCENT)
      .originalSourceScale((short) 6)
      .originalDestinationScale((short) 9)
      .build();

    assertThat(scaledExchangeRate.value())
      .isEqualTo(Ratio.builder().numerator(BigInteger.valueOf(100L)).denominator(BigInteger.TEN).build());
    assertThat(scaledExchangeRate.slippage()).isEqualTo(Slippage.ONE_PERCENT);
    assertThat(scaledExchangeRate.originalSourceScale()).isEqualTo((short) 6);
    assertThat(scaledExchangeRate.originalDestinationScale()).isEqualTo((short) 9);
  }

  @Test
  public void testWhereSourceAndDestAreEqual() {
    ScaledExchangeRate scaledExchangeRate = ScaledExchangeRate.builder()
      .value(Ratio.from(BigDecimal.TEN))
      .slippage(Slippage.ONE_PERCENT)
      .originalSourceScale((short) 9)
      .originalDestinationScale((short) 9)
      .build();

    assertThat(scaledExchangeRate.value())
      .isEqualTo(Ratio.builder().numerator(BigInteger.valueOf(100L)).denominator(BigInteger.TEN).build());
    assertThat(scaledExchangeRate.slippage()).isEqualTo(Slippage.ONE_PERCENT);
    assertThat(scaledExchangeRate.originalSourceScale()).isEqualTo((short) 9);
    assertThat(scaledExchangeRate.originalDestinationScale()).isEqualTo((short) 9);
  }

  @Test
  public void testWhereSourceAndDestAreZero() {
    ScaledExchangeRate scaledExchangeRate = ScaledExchangeRate.builder()
      .value(Ratio.from(BigDecimal.TEN))
      .slippage(Slippage.ONE_PERCENT)
      .originalSourceScale((short) 0)
      .originalDestinationScale((short) 0)
      .build();

    assertThat(scaledExchangeRate.value())
      .isEqualTo(Ratio.builder().numerator(BigInteger.valueOf(100L)).denominator(BigInteger.TEN).build());
    assertThat(scaledExchangeRate.slippage()).isEqualTo(Slippage.ONE_PERCENT);
    assertThat(scaledExchangeRate.originalSourceScale()).isEqualTo((short) 0);
    assertThat(scaledExchangeRate.originalDestinationScale()).isEqualTo((short) 0);
  }

  @Test
  public void testWhereSourceIsZeroAndDestIsNonZero() {
    ScaledExchangeRate scaledExchangeRate = ScaledExchangeRate.builder()
      .value(Ratio.from(BigDecimal.TEN))
      .slippage(Slippage.ONE_PERCENT)
      .originalSourceScale((short) 0)
      .originalDestinationScale((short) 2)
      .build();

    assertThat(scaledExchangeRate.value())
      .isEqualTo(Ratio.builder().numerator(BigInteger.valueOf(100L)).denominator(BigInteger.TEN).build());
    assertThat(scaledExchangeRate.slippage()).isEqualTo(Slippage.ONE_PERCENT);
    assertThat(scaledExchangeRate.originalSourceScale()).isEqualTo((short) 0);
    assertThat(scaledExchangeRate.originalDestinationScale()).isEqualTo((short) 2);
  }

  @Test
  public void testWhereSourceIsNonZeroAndDestIsZero() {
    ScaledExchangeRate scaledExchangeRate = ScaledExchangeRate.builder()
      .value(Ratio.from(BigDecimal.TEN))
      .slippage(Slippage.ONE_PERCENT)
      .originalSourceScale((short) 2)
      .originalDestinationScale((short) 0)
      .build();

    assertThat(scaledExchangeRate.value())
      .isEqualTo(Ratio.builder().numerator(BigInteger.valueOf(100L)).denominator(BigInteger.TEN).build());
    assertThat(scaledExchangeRate.slippage()).isEqualTo(Slippage.ONE_PERCENT);
    assertThat(scaledExchangeRate.originalSourceScale()).isEqualTo((short) 2);
    assertThat(scaledExchangeRate.originalDestinationScale()).isEqualTo((short) 0);
  }

  @Test
  public void testBuildWithZero() {
    ScaledExchangeRate scaledExchangeRate = ScaledExchangeRate.builder()
      .value(Ratio.from(BigDecimal.ZERO))
      .originalSourceScale((short) 9)
      .originalDestinationScale((short) 6)
      .build();

    assertThat(scaledExchangeRate.value().toBigDecimal()).isEqualTo(BigDecimal.ZERO);
    assertThat(scaledExchangeRate.slippage()).isEqualTo(Slippage.ONE_PERCENT);
    assertThat(scaledExchangeRate.originalSourceScale()).isEqualTo((short) 9);
    assertThat(scaledExchangeRate.originalDestinationScale()).isEqualTo((short) 6);
  }

  @Test
  public void testEmptyBuild() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(
      "Cannot build ScaledExchangeRate, some of required attributes are not set "
        + "[value, originalSourceScale, originalDestinationScale]");

    ScaledExchangeRate.builder().build();
  }

  @Test
  public void testToString() {
    ScaledExchangeRate scaledExchangeRate = ScaledExchangeRate.builder()
      .value(Ratio.from(BigDecimal.ONE))
      .originalSourceScale((short) 9)
      .originalDestinationScale((short) 2)
      .build();

    assertThat(scaledExchangeRate.toString()).isEqualTo("ScaledExchangeRate{value=10/10[1], "
      + "originalSourceScale=9, originalDestinationScale=2, slippage=Slippage{value=1%}}");
  }

  @Test
  public void testCompareTo() {
    ScaledExchangeRate scaledExchangeRate0 = ScaledExchangeRate.builder()
      .value(Ratio.from(BigDecimal.ZERO))
      .originalSourceScale((short) 2)
      .originalDestinationScale((short) 2)
      .build();
    ScaledExchangeRate scaledExchangeRate1 = ScaledExchangeRate.builder()
      .value(Ratio.from(BigDecimal.ONE))
      .originalSourceScale((short) 2)
      .originalDestinationScale((short) 2)
      .build();

    assertThat(scaledExchangeRate0.compareTo(scaledExchangeRate0)).isEqualTo(0);
    assertThat(scaledExchangeRate0.compareTo(scaledExchangeRate1)).isEqualTo(-1);
    assertThat(scaledExchangeRate1.compareTo(scaledExchangeRate0)).isEqualTo(1);
  }

  @Test
  public void testEquals() {
    ScaledExchangeRate scaledExchangeRate0 = ScaledExchangeRate.builder()
      .value(Ratio.from(BigDecimal.ZERO))
      .originalSourceScale((short) 2)
      .originalDestinationScale((short) 2)
      .build();
    ScaledExchangeRate scaledExchangeRate1 = ScaledExchangeRate.builder()
      .value(Ratio.from(BigDecimal.ONE))
      .originalSourceScale((short) 2)
      .originalDestinationScale((short) 2)
      .build();

    assertThat(scaledExchangeRate0.equals(scaledExchangeRate0)).isTrue();
    assertThat(scaledExchangeRate0.equals(scaledExchangeRate1)).isFalse();
    assertThat(scaledExchangeRate1.equals(scaledExchangeRate0)).isFalse();
  }

  @Test
  public void testBounds() {
    ScaledExchangeRate scaledExchangeRate = ScaledExchangeRate.builder()
      .value(Ratio.from(new BigDecimal("86.84991150442478")))
      .originalSourceScale((short) 0)
      .originalDestinationScale((short) 0)
      .slippage(Slippage.of(Percentage.of(new BigDecimal("0.00051"))))
      .build();

    assertThat(scaledExchangeRate.slippage().value().value()).isEqualTo(new BigDecimal("0.00051"));

    // Lower Bound / MinRate from JS
    BigDecimal minimumRate = BigDecimal.ONE.subtract(scaledExchangeRate.slippage().value().value())
      .multiply(scaledExchangeRate.value().toBigDecimal());
    assertThat(minimumRate).isEqualTo(new BigDecimal("86.8056180495575233622"));

    // LowerBound
    final BigDecimal lowerBoundFromJS = new BigDecimal("86.8056180495575233622");
    assertThat(scaledExchangeRate.lowerBound().toBigDecimal()).isEqualTo(lowerBoundFromJS);
    assertThat(scaledExchangeRate.lowerBound()).isEqualTo(Ratio.builder()
      .numerator(new BigInteger("868056180495575233622"))
      .denominator(new BigInteger("10000000000000000000"))
      .build());

    // Value
    assertThat(scaledExchangeRate.value().toBigDecimal()).isEqualTo(new BigDecimal("86.84991150442478"));

    // Upper Bound
    assertThat(scaledExchangeRate.upperBound().toBigDecimal()).isEqualTo(new BigDecimal("86.8942049592920366378"));
  }

}