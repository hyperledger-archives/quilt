package org.interledger.core.fluent;


import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.Test;

/**
 * Unit tests for {@link Ratio}.
 */
public class RatioTest {

  @Test
  public void testFrom() {
    Ratio r = Ratio.from(new BigDecimal("86.8056180495575233622"));

    assertThat(r).isEqualTo(Ratio.builder()
      .numerator(new BigInteger("868056180495575233622"))
      .denominator(new BigInteger("10000000000000000000"))
      .build());
  }

  @Test
  public void compareToWhenSmaller() {
    Ratio r1 = Ratio.builder().numerator(BigInteger.valueOf(1)).denominator(BigInteger.valueOf(8)).build();
    Ratio r2 = Ratio.builder().numerator(BigInteger.valueOf(1)).denominator(BigInteger.valueOf(4)).build();

    assertThat(r1.compareTo(r2) < 0).isTrue();
    assertThat(r1.compareTo(r2) == 0).isFalse();
    assertThat(r1.compareTo(r2) > 0).isFalse();
  }

  @Test
  public void compareToWhenEqual() {
    Ratio r1 = Ratio.builder().numerator(BigInteger.valueOf(1)).denominator(BigInteger.valueOf(2)).build();
    Ratio r2 = Ratio.builder().numerator(BigInteger.valueOf(1)).denominator(BigInteger.valueOf(2)).build();

    assertThat(r1.compareTo(r2) < 0).isFalse();
    assertThat(r1.compareTo(r2) == 0).isTrue();
    assertThat(r1.compareTo(r2) > 0).isFalse();
  }

  @Test
  public void compareToWhenEqualWithMorePrecision() {
    Ratio r1 = Ratio.builder().numerator(BigInteger.valueOf(100)).denominator(BigInteger.valueOf(100)).build();
    Ratio r2 = Ratio.builder().numerator(BigInteger.valueOf(10000L)).denominator(BigInteger.valueOf(10000L)).build();

    assertThat(r1.compareTo(r2) < 0).isFalse();
    assertThat(r1.compareTo(r2) == 0).isTrue();
    assertThat(r1.compareTo(r2) > 0).isFalse();
  }

  @Test
  public void compareToWhenBigger() {
    Ratio r1 = Ratio.builder().numerator(BigInteger.valueOf(1)).denominator(BigInteger.valueOf(2)).build();
    Ratio r2 = Ratio.builder().numerator(BigInteger.valueOf(1)).denominator(BigInteger.valueOf(4)).build();

    assertThat(r1.compareTo(r2) < 0).isFalse();
    assertThat(r1.compareTo(r2) == 0).isFalse();
    assertThat(r1.compareTo(r2) > 0).isTrue();
  }

  @Test
  public void comparePrecisionToWhenSmaller() {
    Ratio r1 = Ratio.builder().numerator(BigInteger.valueOf(1)).denominator(BigInteger.valueOf(8)).build();
    Ratio r2 = Ratio.builder().numerator(BigInteger.valueOf(1)).denominator(BigInteger.valueOf(4)).build();

    assertThat(r1.comparePrecisionTo(r2) < 0).isTrue();
    assertThat(r1.comparePrecisionTo(r2) == 0).isFalse();
    assertThat(r1.comparePrecisionTo(r2) > 0).isFalse();
  }

  @Test
  public void comparePrecisionToWhenEqual() {
    Ratio r1 = Ratio.builder().numerator(BigInteger.valueOf(1)).denominator(BigInteger.valueOf(2)).build();
    Ratio r2 = Ratio.builder().numerator(BigInteger.valueOf(1)).denominator(BigInteger.valueOf(2)).build();

    assertThat(r1.comparePrecisionTo(r2) < 0).isFalse();
    assertThat(r1.comparePrecisionTo(r2) == 0).isTrue();
    assertThat(r1.comparePrecisionTo(r2) > 0).isFalse();
  }

  @Test
  public void comparePrecisionToWhenEqualWithMorePrecision() {
    Ratio r1 = Ratio.builder().numerator(BigInteger.valueOf(100)).denominator(BigInteger.valueOf(100)).build();
    Ratio r2 = Ratio.builder().numerator(BigInteger.valueOf(10000L)).denominator(BigInteger.valueOf(10000L)).build();

    assertThat(r1.comparePrecisionTo(r2) < 0).isTrue();
    assertThat(r1.comparePrecisionTo(r2) == 0).isFalse();
    assertThat(r1.comparePrecisionTo(r2) > 0).isFalse();
  }

  @Test
  public void comparePrecisionToWhenBigger() {
    Ratio r1 = Ratio.builder().numerator(BigInteger.valueOf(1)).denominator(BigInteger.valueOf(2)).build();
    Ratio r2 = Ratio.builder().numerator(BigInteger.valueOf(1)).denominator(BigInteger.valueOf(4)).build();

    assertThat(r1.comparePrecisionTo(r2) < 0).isFalse();
    assertThat(r1.comparePrecisionTo(r2) == 0).isFalse();
    assertThat(r1.comparePrecisionTo(r2) > 0).isTrue();
  }

  @Test
  public void toBigDecimal() {
    Ratio r1 = Ratio.builder().numerator(BigInteger.valueOf(1)).denominator(BigInteger.valueOf(2)).build();
    assertThat(r1.toBigDecimal()).isEqualTo(new BigDecimal("0.5"));

    r1 = Ratio.builder().numerator(BigInteger.valueOf(2)).denominator(BigInteger.valueOf(2)).build();
    assertThat(r1.toBigDecimal()).isEqualTo(new BigDecimal("1"));

    r1 = Ratio.builder().numerator(BigInteger.valueOf(1)).denominator(BigInteger.valueOf(Integer.MAX_VALUE)).build();
    assertThat(r1.toBigDecimal()).isEqualTo(new BigDecimal("5e-10"));
  }

  @Test
  public void testSubtract() {
    // From JS
    // lowerBoundRate.subtract(minExchangeRate) ==> 38195044246000 / 100000000000000000

    Ratio minExchangeRateFromJs = Ratio.builder()
      .numerator(BigInteger.valueOf(8680561804955754L))
      .denominator(BigInteger.valueOf(100000000000000L))
      .build();
    Ratio lowerBoundRateFromJS = Ratio.builder()
      .numerator(BigInteger.valueOf(86806L))
      .denominator(BigInteger.valueOf(1000L))
      .build();

    Ratio marginOfErrorFromJs = Ratio.builder()
      .numerator(BigInteger.valueOf(38195044246000L))
      .denominator(BigInteger.valueOf(100000000000000000L))
      .build();
    assertThat(lowerBoundRateFromJS.subtract(minExchangeRateFromJs)).isEqualTo(marginOfErrorFromJs);
  }

  @Test
  public void testRecipricol() {
    Ratio r = Ratio.builder()
      .numerator(BigInteger.valueOf(38195044246000L))
      .denominator(BigInteger.valueOf(100000000000000000L))
      .build();

    Ratio reciprocal = Ratio.builder()
      .numerator(BigInteger.valueOf(100000000000000000L))
      .denominator(BigInteger.valueOf(38195044246000L))
      .build();

    assertThat(r.reciprocal().get()).isEqualTo(reciprocal);
    assertThat(r.reciprocal().get().reciprocal().get()).isEqualTo(r);
    assertThat(reciprocal.reciprocal().get()).isEqualTo(r);
    assertThat(reciprocal.reciprocal().get().reciprocal().get()).isEqualTo(reciprocal);
  }

}