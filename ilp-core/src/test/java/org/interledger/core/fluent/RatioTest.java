package org.interledger.core.fluent;


import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.primitives.UnsignedLong;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Unit tests for {@link Ratio}.
 */
public class RatioTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  ///////////
  // ratio
  ///////////

  @Test
  public void testInvalidRatioZero() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Denominator must be greater-than 0");
    Ratio.builder().numerator(BigInteger.ONE).denominator(BigInteger.ZERO).build();
  }

  @Test
  public void testInvalidRatioNegative() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Denominator must be greater-than 0");
    Ratio.builder().numerator(BigInteger.ONE).denominator(BigInteger.valueOf(-1L)).build();
  }

  @Test
  public void testRatio() {
    Ratio r = Ratio.builder()
      .numerator(BigInteger.ONE)
      .denominator(BigInteger.TEN)
      .build();

    assertThat(r.numerator()).isEqualTo(BigInteger.ONE);
    assertThat(r.denominator()).isEqualTo(BigInteger.TEN);
    assertThat(r.toBigDecimal().toString()).isEqualTo("0.1");
  }

  ///////////
  // testFrom
  ///////////

  @Test
  public void testFrom() {
    Ratio r = Ratio.from(new BigDecimal("86.8056180495575233622"));

    assertThat(r).isEqualTo(Ratio.builder()
      .numerator(new BigInteger("868056180495575233622"))
      .denominator(new BigInteger("10000000000000000000"))
      .build());
  }

  ///////////
  // compareTo
  ///////////

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

  ///////////
  // toBigDecimal
  ///////////

  @Test
  public void toBigDecimal() {
    Ratio r1 = Ratio.builder().numerator(BigInteger.valueOf(1)).denominator(BigInteger.valueOf(2)).build();
    assertThat(r1.toBigDecimal()).isEqualTo(new BigDecimal("0.5"));

    r1 = Ratio.builder().numerator(BigInteger.valueOf(2)).denominator(BigInteger.valueOf(2)).build();
    assertThat(r1.toBigDecimal()).isEqualTo(new BigDecimal("1"));

    r1 = Ratio.builder().numerator(BigInteger.valueOf(1)).denominator(BigInteger.valueOf(Integer.MAX_VALUE)).build();
    assertThat(r1.toBigDecimal()).isEqualTo(new BigDecimal("4.656612875245796924105750827167998E-10"));
  }

  ///////////
  // equalButMorePrecise
  ///////////

  @Test
  public void testEqualButMorePreciseWhenSmaller() {
    Ratio r1 = Ratio.builder().numerator(BigInteger.valueOf(1)).denominator(BigInteger.valueOf(8)).build();
    Ratio r2 = Ratio.builder().numerator(BigInteger.valueOf(1)).denominator(BigInteger.valueOf(4)).build();

    assertThat(r1.equalButMorePrecise((r2))).isFalse();
    assertThat(r2.equalButMorePrecise(r1)).isFalse();
  }

  @Test
  public void testEqualButMorePreciseWhenEqual() {
    Ratio r1 = Ratio.builder().numerator(BigInteger.valueOf(1)).denominator(BigInteger.valueOf(2)).build();
    Ratio r2 = Ratio.builder().numerator(BigInteger.valueOf(1)).denominator(BigInteger.valueOf(2)).build();

    assertThat(r1.equalButMorePrecise((r2))).isFalse();
    assertThat(r2.equalButMorePrecise(r1)).isFalse();
  }

  @Test
  public void testEqualButMorePreciseWhenMorePrecise() {
    Ratio r1 = Ratio.builder().numerator(BigInteger.valueOf(100)).denominator(BigInteger.valueOf(100)).build();
    Ratio r2 = Ratio.builder().numerator(BigInteger.valueOf(10000L)).denominator(BigInteger.valueOf(10000L)).build();

    assertThat(r1.equalButMorePrecise((r2))).isFalse();
    assertThat(r2.equalButMorePrecise(r1)).isTrue();
  }

  @Test
  public void testEqualButMorePreciseWhenBigger() {
    Ratio r1 = Ratio.builder().numerator(BigInteger.valueOf(1)).denominator(BigInteger.valueOf(2)).build();
    Ratio r2 = Ratio.builder().numerator(BigInteger.valueOf(1)).denominator(BigInteger.valueOf(4)).build();

    assertThat(r1.equalButMorePrecise((r2))).isFalse();
    assertThat(r2.equalButMorePrecise(r1)).isFalse();
  }

  ///////////
  // multiplyFloor
  ///////////

  @Test
  public void multiplyFloor() {
    Ratio r = Ratio.ONE;
    assertThat(r.multiplyFloor(BigInteger.valueOf(3L))).isEqualTo(BigInteger.valueOf(3L));
    assertThat(r.multiplyFloor(BigInteger.valueOf(5L))).isEqualTo(BigInteger.valueOf(5L));
    assertThat(r.multiplyFloor(BigInteger.ZERO)).isEqualTo(BigInteger.ZERO);
    assertThat(r.multiplyFloor(BigInteger.ONE)).isEqualTo(BigInteger.ONE);

    r = Ratio.ZERO;
    assertThat(r.multiplyFloor(BigInteger.valueOf(3L))).isEqualTo(BigInteger.ZERO);
    assertThat(r.multiplyFloor(BigInteger.valueOf(5L))).isEqualTo(BigInteger.ZERO);
    assertThat(r.multiplyFloor(BigInteger.ZERO)).isEqualTo(BigInteger.ZERO);
    assertThat(r.multiplyFloor(BigInteger.ONE)).isEqualTo(BigInteger.ZERO);

    r = Ratio.builder()
      .numerator(BigInteger.valueOf(1L))
      .denominator(BigInteger.valueOf(2L))
      .build();
    assertThat(r.multiplyFloor(BigInteger.valueOf(3L))).isEqualTo(BigInteger.ONE);
    assertThat(r.multiplyFloor(BigInteger.valueOf(5L))).isEqualTo(BigInteger.valueOf(2L));
    assertThat(r.multiplyFloor(BigInteger.ZERO)).isEqualTo(BigInteger.ZERO);
    assertThat(r.multiplyFloor(BigInteger.ONE)).isEqualTo(BigInteger.ZERO);

    r = Ratio.builder()
      .numerator(BigInteger.valueOf(3L))
      .denominator(BigInteger.valueOf(2L))
      .build();
    assertThat(r.multiplyFloor(BigInteger.valueOf(3L))).isEqualTo(BigInteger.valueOf(4L));
    assertThat(r.multiplyFloor(BigInteger.valueOf(5L))).isEqualTo(BigInteger.valueOf(7L));
    assertThat(r.multiplyFloor(BigInteger.ZERO)).isEqualTo(BigInteger.ZERO);
    assertThat(r.multiplyFloor(BigInteger.ONE)).isEqualTo(BigInteger.ONE);

    r = Ratio.builder()
      .numerator(BigInteger.valueOf(1L))
      .denominator(UnsignedLong.MAX_VALUE.bigIntegerValue())
      .build();
    assertThat(r.multiplyFloor(BigInteger.valueOf(3L))).isEqualTo(BigInteger.ZERO);
    assertThat(r.multiplyFloor(BigInteger.valueOf(5L))).isEqualTo(BigInteger.ZERO);
    assertThat(r.multiplyFloor(BigInteger.ZERO)).isEqualTo(BigInteger.ZERO);
    assertThat(r.multiplyFloor(BigInteger.ONE)).isEqualTo(BigInteger.ZERO);
  }

  @Test
  public void multiplyFloorBigIntegerLongWithOverflow() {
    Ratio r = Ratio.builder().numerator(BigInteger.TEN).denominator(BigInteger.ONE).build();
    assertThat(r.multiplyFloor(UnsignedLong.MAX_VALUE.bigIntegerValue()))
      .isEqualTo(new BigInteger("184467440737095516150"));
  }

  ///////////
  // multiplyFloorOrZero
  ///////////

  @Test
  public void multiplyFloorOrZero() {
    Ratio r = Ratio.ONE;
    assertThat(r.multiplyFloorOrZero(UnsignedLong.valueOf(3L))).isEqualTo(UnsignedLong.valueOf(3L));
    assertThat(r.multiplyFloorOrZero(UnsignedLong.valueOf(5L))).isEqualTo(UnsignedLong.valueOf(5L));
    assertThat(r.multiplyFloorOrZero(UnsignedLong.ZERO)).isEqualTo(UnsignedLong.ZERO);
    assertThat(r.multiplyFloorOrZero(UnsignedLong.ONE)).isEqualTo(UnsignedLong.ONE);

    r = Ratio.ZERO;
    assertThat(r.multiplyFloorOrZero(UnsignedLong.valueOf(3L))).isEqualTo(UnsignedLong.ZERO);
    assertThat(r.multiplyFloorOrZero(UnsignedLong.valueOf(5L))).isEqualTo(UnsignedLong.ZERO);
    assertThat(r.multiplyFloorOrZero(UnsignedLong.ZERO)).isEqualTo(UnsignedLong.ZERO);
    assertThat(r.multiplyFloorOrZero(UnsignedLong.ONE)).isEqualTo(UnsignedLong.ZERO);

    r = Ratio.builder()
      .numerator(BigInteger.valueOf(1L))
      .denominator(BigInteger.valueOf(2L))
      .build();
    assertThat(r.multiplyFloorOrZero(UnsignedLong.valueOf(3L))).isEqualTo(UnsignedLong.ONE);
    assertThat(r.multiplyFloorOrZero(UnsignedLong.valueOf(5L))).isEqualTo(UnsignedLong.valueOf(2L));
    assertThat(r.multiplyFloorOrZero(UnsignedLong.ZERO)).isEqualTo(UnsignedLong.ZERO);
    assertThat(r.multiplyFloorOrZero(UnsignedLong.ONE)).isEqualTo(UnsignedLong.ZERO);

    r = Ratio.builder()
      .numerator(BigInteger.valueOf(3L))
      .denominator(BigInteger.valueOf(2L))
      .build();
    assertThat(r.multiplyFloorOrZero(UnsignedLong.valueOf(3L))).isEqualTo(UnsignedLong.valueOf(4L));
    assertThat(r.multiplyFloorOrZero(UnsignedLong.valueOf(5L))).isEqualTo(UnsignedLong.valueOf(7L));
    assertThat(r.multiplyFloorOrZero(UnsignedLong.ZERO)).isEqualTo(UnsignedLong.ZERO);
    assertThat(r.multiplyFloorOrZero(UnsignedLong.ONE)).isEqualTo(UnsignedLong.ONE);

    r = Ratio.builder()
      .numerator(BigInteger.valueOf(1L))
      .denominator(UnsignedLong.MAX_VALUE.bigIntegerValue())
      .build();
    assertThat(r.multiplyFloorOrZero(UnsignedLong.valueOf(3L))).isEqualTo(UnsignedLong.ZERO);
    assertThat(r.multiplyFloorOrZero(UnsignedLong.valueOf(5L))).isEqualTo(UnsignedLong.ZERO);
    assertThat(r.multiplyFloorOrZero(UnsignedLong.ZERO)).isEqualTo(UnsignedLong.ZERO);
    assertThat(r.multiplyFloorOrZero(UnsignedLong.ONE)).isEqualTo(UnsignedLong.ZERO);
  }

  @Test
  public void multiplyFloorOrZeroWithOverflow() {
    Ratio r = Ratio.builder().numerator(BigInteger.TEN).denominator(BigInteger.ONE).build();
    assertThat(r.multiplyFloorOrZero(UnsignedLong.MAX_VALUE)).isEqualTo(UnsignedLong.ZERO);
  }

  ///////////
  // multiplyCeil
  ///////////

  @Test
  public void multiplyCeilBigInteger() {
    Ratio r = Ratio.ONE;
    assertThat(r.multiplyCeil(BigInteger.valueOf(3L))).isEqualTo(BigInteger.valueOf(3L));
    assertThat(r.multiplyCeil(BigInteger.valueOf(5L))).isEqualTo(BigInteger.valueOf(5L));
    assertThat(r.multiplyCeil(BigInteger.ZERO)).isEqualTo(BigInteger.ZERO);
    assertThat(r.multiplyCeil(BigInteger.ONE)).isEqualTo(BigInteger.ONE);

    r = Ratio.ZERO;
    assertThat(r.multiplyCeil(BigInteger.valueOf(3L))).isEqualTo(BigInteger.ZERO);
    assertThat(r.multiplyCeil(BigInteger.valueOf(5L))).isEqualTo(BigInteger.ZERO);
    assertThat(r.multiplyCeil(BigInteger.ZERO)).isEqualTo(BigInteger.ZERO);
    assertThat(r.multiplyCeil(BigInteger.ONE)).isEqualTo(BigInteger.ZERO);

    r = Ratio.builder()
      .numerator(BigInteger.valueOf(1L))
      .denominator(BigInteger.valueOf(2L))
      .build();
    assertThat(r.multiplyCeil(BigInteger.valueOf(3L))).isEqualTo(BigInteger.valueOf(2L));
    assertThat(r.multiplyCeil(BigInteger.valueOf(5L))).isEqualTo(BigInteger.valueOf(3L));
    assertThat(r.multiplyCeil(BigInteger.ZERO)).isEqualTo(BigInteger.ZERO);
    assertThat(r.multiplyCeil(BigInteger.ONE)).isEqualTo(BigInteger.ONE);

    r = Ratio.builder()
      .numerator(BigInteger.valueOf(3L))
      .denominator(BigInteger.valueOf(2L))
      .build();
    assertThat(r.multiplyCeil(BigInteger.valueOf(3L))).isEqualTo(BigInteger.valueOf(5L));
    assertThat(r.multiplyCeil(BigInteger.valueOf(5L))).isEqualTo(BigInteger.valueOf(8L));
    assertThat(r.multiplyCeil(BigInteger.ZERO)).isEqualTo(BigInteger.ZERO);
    assertThat(r.multiplyCeil(BigInteger.ONE)).isEqualTo(BigInteger.valueOf(2L));

    r = Ratio.builder()
      .numerator(BigInteger.valueOf(1L))
      .denominator(UnsignedLong.MAX_VALUE.bigIntegerValue())
      .build();
    assertThat(r.multiplyCeil(BigInteger.valueOf(3L))).isEqualTo(BigInteger.ONE);
    assertThat(r.multiplyCeil(BigInteger.valueOf(5L))).isEqualTo(BigInteger.ONE);
    assertThat(r.multiplyCeil(BigInteger.ZERO)).isEqualTo(BigInteger.ZERO);
    assertThat(r.multiplyCeil(BigInteger.ONE)).isEqualTo(BigInteger.ONE);
  }

  @Test
  public void multiplyCeilBigIntegerWithOverflow() {
    Ratio r = Ratio.builder().numerator(BigInteger.TEN).denominator(BigInteger.ONE).build();
    assertThat(r.multiplyCeil(UnsignedLong.MAX_VALUE.bigIntegerValue()))
      .isEqualTo(new BigInteger("184467440737095516150"));
  }

  ///////////
  // multiplyCeilOrZero
  ///////////

  @Test
  public void multiplyCeilUnsignedLong() {
    Ratio r = Ratio.ONE;
    assertThat(r.multiplyCeilOrZero(UnsignedLong.valueOf(3L))).isEqualTo(UnsignedLong.valueOf(3L));
    assertThat(r.multiplyCeilOrZero(UnsignedLong.valueOf(5L))).isEqualTo(UnsignedLong.valueOf(5L));
    assertThat(r.multiplyCeilOrZero(UnsignedLong.ZERO)).isEqualTo(UnsignedLong.ZERO);
    assertThat(r.multiplyCeilOrZero(UnsignedLong.ONE)).isEqualTo(UnsignedLong.ONE);

    r = Ratio.ZERO;
    assertThat(r.multiplyCeilOrZero(UnsignedLong.valueOf(3L))).isEqualTo(UnsignedLong.ZERO);
    assertThat(r.multiplyCeilOrZero(UnsignedLong.valueOf(5L))).isEqualTo(UnsignedLong.ZERO);
    assertThat(r.multiplyCeilOrZero(UnsignedLong.ZERO)).isEqualTo(UnsignedLong.ZERO);
    assertThat(r.multiplyCeilOrZero(UnsignedLong.ONE)).isEqualTo(UnsignedLong.ZERO);

    r = Ratio.builder()
      .numerator(BigInteger.valueOf(1L))
      .denominator(BigInteger.valueOf(2L))
      .build();
    assertThat(r.multiplyCeilOrZero(UnsignedLong.valueOf(3L))).isEqualTo(UnsignedLong.valueOf(2L));
    assertThat(r.multiplyCeilOrZero(UnsignedLong.valueOf(5L))).isEqualTo(UnsignedLong.valueOf(3L));
    assertThat(r.multiplyCeilOrZero(UnsignedLong.ZERO)).isEqualTo(UnsignedLong.ZERO);
    assertThat(r.multiplyCeilOrZero(UnsignedLong.ONE)).isEqualTo(UnsignedLong.ONE);

    r = Ratio.builder()
      .numerator(BigInteger.valueOf(3L))
      .denominator(BigInteger.valueOf(2L))
      .build();
    assertThat(r.multiplyCeilOrZero(UnsignedLong.valueOf(3L))).isEqualTo(UnsignedLong.valueOf(5L));
    assertThat(r.multiplyCeilOrZero(UnsignedLong.valueOf(5L))).isEqualTo(UnsignedLong.valueOf(8L));
    assertThat(r.multiplyCeilOrZero(UnsignedLong.ZERO)).isEqualTo(UnsignedLong.ZERO);
    assertThat(r.multiplyCeilOrZero(UnsignedLong.ONE)).isEqualTo(UnsignedLong.valueOf(2L));

    r = Ratio.builder()
      .numerator(BigInteger.valueOf(1L))
      .denominator(UnsignedLong.MAX_VALUE.bigIntegerValue())
      .build();
    assertThat(r.multiplyCeilOrZero(UnsignedLong.valueOf(3L))).isEqualTo(UnsignedLong.ONE);
    assertThat(r.multiplyCeilOrZero(UnsignedLong.valueOf(5L))).isEqualTo(UnsignedLong.ONE);
    assertThat(r.multiplyCeilOrZero(UnsignedLong.ZERO)).isEqualTo(UnsignedLong.ZERO);
    assertThat(r.multiplyCeilOrZero(UnsignedLong.ONE)).isEqualTo(UnsignedLong.ONE);
  }

  @Test
  public void multiplyCeilUnsignedLongWithOverflow() {
    Ratio r = Ratio.builder().numerator(BigInteger.TEN).denominator(BigInteger.ONE).build();
    assertThat(r.multiplyCeilOrZero(UnsignedLong.MAX_VALUE)).isEqualTo(UnsignedLong.ZERO);
  }

  ///////////
  // reciprocal
  ///////////

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

  @Test
  public void testRecipricolWhenNegative() {
    Ratio r = Ratio.builder()
      .numerator(BigInteger.valueOf(-8195044246000L))
      .denominator(BigInteger.valueOf(100000000000000000L))
      .build();

    assertThat(r.reciprocal()).isEmpty();
  }

  ///////////
  // subtract
  ///////////

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
  public void testSubtractNegative() {
    Ratio small = Ratio.builder()
      .numerator(BigInteger.valueOf(8680561804955754L))
      .denominator(BigInteger.valueOf(100000000000000L))
      .build();
    Ratio big = Ratio.builder()
      .numerator(BigInteger.valueOf(86806L))
      .denominator(BigInteger.valueOf(1000L))
      .build();

    Ratio difference = Ratio.builder()
      .numerator(BigInteger.valueOf(-38195044246000L))
      .denominator(BigInteger.valueOf(100000000000000000L))
      .build();

    assertThat(small.subtract(big)).isEqualTo(difference);
  }

  ///////////
  // isPositive
  ///////////

  @Test
  public void isPositive() {
    assertThat(Ratio.ONE.isPositive()).isTrue();
    assertThat(Ratio.ZERO.isNotPositive()).isTrue();
    assertThat(Ratio.builder().numerator(BigInteger.ONE).denominator(BigInteger.valueOf(10L)).build().isPositive())
      .isTrue();
    assertThat(
      Ratio.builder().numerator(BigInteger.valueOf(-1L)).denominator(BigInteger.valueOf(10L)).build().isPositive()
    ).isFalse();
    assertThat(Ratio.builder().numerator(BigInteger.valueOf(-10)).denominator(BigInteger.ONE).build().isPositive())
      .isFalse();
  }

  ///////////
  // isNotPositive
  ///////////

  @Test
  public void isNotPositive() {
    assertThat(Ratio.ONE.isNotPositive()).isFalse();
    assertThat(Ratio.ZERO.isNotPositive()).isTrue();
    assertThat(Ratio.builder().numerator(BigInteger.ONE).denominator(BigInteger.valueOf(10L)).build().isNotPositive())
      .isFalse();
    assertThat(
      Ratio.builder().numerator(BigInteger.valueOf(-1L)).denominator(BigInteger.valueOf(10L)).build().isNotPositive()
    ).isTrue();
    assertThat(Ratio.builder().numerator(BigInteger.valueOf(-10)).denominator(BigInteger.ONE).build().isNotPositive())
      .isTrue();
  }

  ///////////
  // isInteger
  ///////////

  @Test
  public void isInteger() {
    assertThat(Ratio.ONE.isInteger()).isTrue();
    assertThat(Ratio.ZERO.isInteger()).isTrue();
    assertThat(
      Ratio.builder().numerator(BigInteger.ONE).denominator(BigInteger.valueOf(10L)).build().isInteger()
    ).isFalse();
    assertThat(
      Ratio.builder().numerator(BigInteger.valueOf(-1L)).denominator(BigInteger.valueOf(10L)).build().isInteger()
    ).isFalse();
    assertThat(
      Ratio.builder().numerator(BigInteger.valueOf(-10)).denominator(BigInteger.ONE).build().isInteger()
    ).isTrue();
  }

  ///////////
  // isPositiveInteger
  ///////////

  @Test
  public void isPositiveInteger() {
    assertThat(Ratio.ONE.isPositiveInteger()).isTrue();
    assertThat(Ratio.ZERO.isPositiveInteger()).isFalse();
    assertThat(
      Ratio.builder().numerator(BigInteger.ONE).denominator(BigInteger.valueOf(10L)).build().isPositiveInteger()
    ).isFalse();
    assertThat(
      Ratio.builder().numerator(BigInteger.valueOf(-1L)).denominator(BigInteger.valueOf(10L)).build()
        .isPositiveInteger()
    ).isFalse();
    assertThat(
      Ratio.builder().numerator(BigInteger.valueOf(-10)).denominator(BigInteger.ONE).build().isPositiveInteger()
    ).isFalse();
  }

  ///////////
  // isNegative
  ///////////
  @Test
  public void isNegative() {
    assertThat(Ratio.ONE.isNegative()).isFalse();
    assertThat(Ratio.ZERO.isNegative()).isFalse();
    assertThat(Ratio.builder().numerator(BigInteger.ONE).denominator(BigInteger.valueOf(10L)).build().isNegative())
      .isFalse();
    assertThat(
      Ratio.builder().numerator(BigInteger.valueOf(-1L)).denominator(BigInteger.valueOf(10L)).build().isNegative()
    ).isTrue();
    assertThat(Ratio.builder().numerator(BigInteger.valueOf(-10)).denominator(BigInteger.ONE).build().isNegative())
      .isTrue();
  }

  ///////////
  // isZero
  ///////////

  @Test
  public void isZero() {
    assertThat(Ratio.ONE.isZero()).isFalse();
    assertThat(Ratio.ZERO.isZero()).isTrue();
    assertThat(Ratio.builder().numerator(BigInteger.ONE).denominator(BigInteger.valueOf(10L)).build().isZero())
      .isFalse();
    assertThat(
      Ratio.builder().numerator(BigInteger.valueOf(-1L)).denominator(BigInteger.valueOf(10L)).build().isZero()
    ).isFalse();
    assertThat(Ratio.builder().numerator(BigInteger.valueOf(-10)).denominator(BigInteger.ONE).build().isZero())
      .isFalse();
  }

  ///////////
  // from(BigDecimal)
  ///////////

  @Test
  public void fromBigDecimal() {
    final Ratio r = Ratio.from(new BigDecimal("0.1"));
    assertThat(r.numerator()).isEqualTo(BigInteger.valueOf(1L));
    assertThat(r.denominator()).isEqualTo(BigInteger.valueOf(10L));
  }

  @Test
  public void fromBigDecimalZero() {
    final Ratio r = Ratio.from(new BigDecimal("0.0"));
    assertThat(r.numerator()).isEqualTo(BigInteger.valueOf(0L));
    assertThat(r.denominator()).isEqualTo(BigInteger.valueOf(10L));
  }

  @Test
  public void fromBigDecimalOne() {
    final Ratio r = Ratio.from(new BigDecimal("1.0"));
    assertThat(r.numerator()).isEqualTo(BigInteger.valueOf(10L));
    assertThat(r.denominator()).isEqualTo(BigInteger.valueOf(10L));
  }

  ///////////
  // from(BigInteger numerator, BigInteger denominator)
  ///////////

  @Test
  public void fromNumeratorDenominator1Over10() {
    final Ratio r = Ratio.from(BigInteger.valueOf(1L), BigInteger.valueOf(10L));
    assertThat(r.numerator()).isEqualTo(BigInteger.valueOf(1L));
    assertThat(r.denominator()).isEqualTo(BigInteger.valueOf(10L));
  }

  @Test
  public void fromNumeratorDenominator0Over10() {
    final Ratio r = Ratio.from(BigInteger.valueOf(0L), BigInteger.valueOf(10L));
    assertThat(r.numerator()).isEqualTo(BigInteger.valueOf(0L));
    assertThat(r.denominator()).isEqualTo(BigInteger.valueOf(10L));
  }

  @Test
  public void fromNumeratorDenominator10Over10() {
    final Ratio r = Ratio.from(BigInteger.valueOf(10L), BigInteger.valueOf(10L));
    assertThat(r.numerator()).isEqualTo(BigInteger.valueOf(10L));
    assertThat(r.denominator()).isEqualTo(BigInteger.valueOf(10L));
  }

  ///////////
  // from(UnsignedLong numerator, UnsignedLong denominator)
  ///////////

  @Test
  public void fromULNumeratorDenominator1Over10() {
    final Ratio r = Ratio.from(UnsignedLong.valueOf(1L), UnsignedLong.valueOf(10L));
    assertThat(r.numerator()).isEqualTo(BigInteger.valueOf(1L));
    assertThat(r.denominator()).isEqualTo(BigInteger.valueOf(10L));
  }

  @Test
  public void fromULNumeratorDenominator0Over10() {
    final Ratio r = Ratio.from(UnsignedLong.valueOf(0L), UnsignedLong.valueOf(10L));
    assertThat(r.numerator()).isEqualTo(BigInteger.valueOf(0L));
    assertThat(r.denominator()).isEqualTo(BigInteger.valueOf(10L));
  }

  @Test
  public void fromULNumeratorDenominator10Over10() {
    final Ratio r = Ratio.from(UnsignedLong.valueOf(10L), UnsignedLong.valueOf(10L));
    assertThat(r.numerator()).isEqualTo(BigInteger.valueOf(10L));
    assertThat(r.denominator()).isEqualTo(BigInteger.valueOf(10L));
  }

  ///////////
  // toString
  ///////////

  @Test
  public void testToString() {
    assertThat(Ratio.from(UnsignedLong.valueOf(10L), UnsignedLong.valueOf(9L)).toString())
      .isEqualTo("10/9[1.111111111111111111111111111111111]");

    assertThat(Ratio.from(UnsignedLong.valueOf(1L), UnsignedLong.valueOf(2L)).toString())
      .isEqualTo("1/2[0.5]");

    assertThat(Ratio.from(UnsignedLong.valueOf(1L), UnsignedLong.valueOf(1L)).toString())
      .isEqualTo("1/1[1]");
  }
}