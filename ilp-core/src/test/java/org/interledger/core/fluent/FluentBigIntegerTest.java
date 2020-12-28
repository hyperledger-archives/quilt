package org.interledger.core.fluent;


import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.google.common.primitives.UnsignedLong;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Unit tests for {@link FluentBigInteger}.
 */
public class FluentBigIntegerTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  ///////////
  // testOf
  ///////////

  @Test
  public void testOfWithNull() {
    expectedException.expect(NullPointerException.class);
    FluentBigInteger.of(null);
  }

  @Test
  public void testOf() {
    assertThat(FluentBigInteger.of(BigInteger.ONE).getValue()).isEqualTo(BigInteger.ONE);
  }

  ///////////
  // timesFloor (Ratio)
  ///////////

  @Test
  public void timesFloor() {
    Ratio r = Ratio.from(new BigDecimal("0.2500001").subtract(new BigDecimal("0.2500000")));
    BigInteger actual = FluentBigInteger.of(BigInteger.ONE)
      .timesFloor(r)
      .getValue();
    assertThat(actual).isEqualTo(BigInteger.valueOf(0L));

    r = Ratio.from(new BigDecimal("0.0001"));
    actual = FluentBigInteger.of(BigInteger.ONE)
      .timesFloor(r)
      .getValue();
    assertThat(actual).isEqualTo(BigInteger.ZERO);

    r = Ratio.from(new BigDecimal("0.001"));
    actual = FluentBigInteger.of(BigInteger.ONE)
      .timesFloor(r)
      .getValue();
    assertThat(actual).isEqualTo(BigInteger.ZERO);

    r = Ratio.from(new BigDecimal("0.333"));
    actual = FluentBigInteger.of(BigInteger.ONE)
      .timesFloor(r)
      .getValue();
    assertThat(actual).isEqualTo(BigInteger.ZERO);

    r = Ratio.from(new BigDecimal("0.999"));
    actual = FluentBigInteger.of(BigInteger.ONE)
      .timesFloor(r)
      .getValue();
    assertThat(actual).isEqualTo(BigInteger.ZERO);

    r = Ratio.from(new BigDecimal("1.0"));
    actual = FluentBigInteger.of(BigInteger.ONE)
      .timesFloor(r)
      .getValue();
    assertThat(actual).isEqualTo(BigInteger.valueOf(1L));

    r = Ratio.from(new BigDecimal("1.9"));
    actual = FluentBigInteger.of(BigInteger.ONE)
      .timesFloor(r)
      .getValue();
    assertThat(actual).isEqualTo(BigInteger.valueOf(1L));

    r = Ratio.from(new BigDecimal("10.9"));
    actual = FluentBigInteger.of(BigInteger.ONE)
      .timesFloor(r)
      .getValue();
    assertThat(actual).isEqualTo(BigInteger.TEN);
  }

  ///////////
  // timesCeil (Ratio)
  ///////////

  @Test
  public void timesCeilRatioNull() {
    expectedException.expect(NullPointerException.class);
    Ratio r = null;
    FluentBigInteger.of(BigInteger.ONE).timesCeil(r);
  }

  @Test
  public void timesCeil() {
    Ratio r = Ratio.from(new BigDecimal("0.2500001").subtract(new BigDecimal("0.2500000")));
    BigInteger actual = FluentBigInteger.of(BigInteger.ONE)
      .timesCeil(r)
      .getValue();
    assertThat(actual).isEqualTo(BigInteger.valueOf(1L));

    r = Ratio.from(new BigDecimal("0.0001"));
    actual = FluentBigInteger.of(BigInteger.ONE)
      .timesCeil(r)
      .getValue();
    assertThat(actual).isEqualTo(BigInteger.ONE);

    r = Ratio.from(new BigDecimal("0.001"));
    actual = FluentBigInteger.of(BigInteger.ONE)
      .timesCeil(r)
      .getValue();
    assertThat(actual).isEqualTo(BigInteger.ONE);

    r = Ratio.from(new BigDecimal("0.333"));
    actual = FluentBigInteger.of(BigInteger.ONE)
      .timesCeil(r)
      .getValue();
    assertThat(actual).isEqualTo(BigInteger.ONE);

    r = Ratio.from(new BigDecimal("0.999"));
    actual = FluentBigInteger.of(BigInteger.ONE)
      .timesCeil(r)
      .getValue();
    assertThat(actual).isEqualTo(BigInteger.ONE);

    r = Ratio.from(new BigDecimal("1.0"));
    actual = FluentBigInteger.of(BigInteger.ONE)
      .timesCeil(r)
      .getValue();
    assertThat(actual).isEqualTo(BigInteger.ONE);

    r = Ratio.from(new BigDecimal("1.9"));
    actual = FluentBigInteger.of(BigInteger.ONE)
      .timesCeil(r)
      .getValue();
    assertThat(actual).isEqualTo(BigInteger.valueOf(2L));

    r = Ratio.from(new BigDecimal("10.9"));
    actual = FluentBigInteger.of(BigInteger.ONE)
      .timesCeil(r)
      .getValue();
    assertThat(actual).isEqualTo(BigInteger.valueOf(11L));
  }

  ///////////
  // timesCeil (BigDecimal)
  ///////////

  @Test
  public void timesCeilBigDecimalNull() {
    expectedException.expect(NullPointerException.class);
    BigDecimal bd = null;
    FluentBigInteger.of(BigInteger.ONE).timesCeil(bd);
  }

  @Test
  public void timesCeilBigDecimal() {
    BigDecimal bd = Ratio.from(new BigDecimal("0.2500001").subtract(new BigDecimal("0.2500000"))).toBigDecimal();
    BigInteger actual = FluentBigInteger.of(BigInteger.ONE)
      .timesCeil(bd)
      .getValue();
    assertThat(actual).isEqualTo(BigInteger.valueOf(1L));

    bd = Ratio.from(new BigDecimal("0.0001")).toBigDecimal();
    actual = FluentBigInteger.of(BigInteger.ONE)
      .timesCeil(bd)
      .getValue();
    assertThat(actual).isEqualTo(BigInteger.ONE);

    bd = Ratio.from(new BigDecimal("0.001")).toBigDecimal();
    actual = FluentBigInteger.of(BigInteger.ONE)
      .timesCeil(bd)
      .getValue();
    assertThat(actual).isEqualTo(BigInteger.ONE);

    bd = Ratio.from(new BigDecimal("0.333")).toBigDecimal();
    actual = FluentBigInteger.of(BigInteger.ONE)
      .timesCeil(bd)
      .getValue();
    assertThat(actual).isEqualTo(BigInteger.ONE);

    bd = Ratio.from(new BigDecimal("0.999")).toBigDecimal();
    actual = FluentBigInteger.of(BigInteger.ONE)
      .timesCeil(bd)
      .getValue();
    assertThat(actual).isEqualTo(BigInteger.ONE);

    bd = Ratio.from(new BigDecimal("1.0")).toBigDecimal();
    actual = FluentBigInteger.of(BigInteger.ONE)
      .timesCeil(bd)
      .getValue();
    assertThat(actual).isEqualTo(BigInteger.ONE);

    bd = Ratio.from(new BigDecimal("1.9")).toBigDecimal();
    actual = FluentBigInteger.of(BigInteger.ONE)
      .timesCeil(bd)
      .getValue();
    assertThat(actual).isEqualTo(BigInteger.valueOf(2L));

    bd = Ratio.from(new BigDecimal("10.9")).toBigDecimal();
    actual = FluentBigInteger.of(BigInteger.ONE)
      .timesCeil(bd)
      .getValue();
    assertThat(actual).isEqualTo(BigInteger.valueOf(11L));
  }

  ///////////
  // dividCeil (BigInteger)
  ///////////

  @Test
  public void divideCeilBigDecimalNull() {
    expectedException.expect(NullPointerException.class);
    BigInteger bd = null;
    FluentBigInteger.of(BigInteger.ONE).divideCeil(bd);
  }

  @Test
  public void divideCeilBigDecimal() {
    assertThat(FluentBigInteger.of(BigInteger.ZERO).divideCeil(BigInteger.ZERO).getValue()).isEqualTo(BigInteger.ZERO);
    assertThat(FluentBigInteger.of(BigInteger.ONE).divideCeil(BigInteger.ZERO).getValue()).isEqualTo(BigInteger.ZERO);
    assertThat(FluentBigInteger.of(BigInteger.ZERO).divideCeil(BigInteger.ONE).getValue()).isEqualTo(BigInteger.ZERO);
    assertThat(FluentBigInteger.of(BigInteger.ONE).divideCeil(BigInteger.ONE).getValue()).isEqualTo(BigInteger.ONE);
    assertThat(FluentBigInteger.of(BigInteger.ONE).divideCeil(BigInteger.valueOf(2L)).getValue())
      .isEqualTo(BigInteger.ONE);
    assertThat(FluentBigInteger.of(BigInteger.ONE).divideCeil(BigInteger.valueOf(3L)).getValue())
      .isEqualTo(BigInteger.ONE);
    assertThat(FluentBigInteger.of(BigInteger.TEN).divideCeil(BigInteger.ONE).getValue()).isEqualTo(BigInteger.TEN);
    assertThat(FluentBigInteger.of(BigInteger.TEN).divideCeil(BigInteger.valueOf(9L)).getValue())
      .isEqualTo(BigInteger.valueOf(2L));
  }

  ///////////
  // isPositive
  ///////////

  @Test
  public void isPositive() {
    assertThat(FluentBigInteger.of(BigInteger.valueOf(-1L)).isPositive()).isFalse();
    assertThat(FluentBigInteger.of(BigInteger.ZERO).isPositive()).isFalse();
    assertThat(FluentBigInteger.of(BigInteger.ONE).isPositive()).isTrue();
  }

  @Test
  public void isNotPositive() {
    assertThat(FluentBigInteger.of(BigInteger.valueOf(-1L)).isNotPositive()).isTrue();
    assertThat(FluentBigInteger.of(BigInteger.ZERO).isNotPositive()).isTrue();
    assertThat(FluentBigInteger.of(BigInteger.ONE).isNotPositive()).isFalse();
  }

  ///////////
  // orMaxUnsignedLong
  ///////////

  @Test
  public void orMaxUnsignedLong() {
    assertThat(FluentBigInteger.of(BigInteger.ZERO).orMaxUnsignedLong()).isEqualTo(UnsignedLong.ZERO);
    assertThat(FluentBigInteger.of(BigInteger.ONE).orMaxUnsignedLong()).isEqualTo(UnsignedLong.ONE);
    assertThat(FluentBigInteger.of(UnsignedLong.MAX_VALUE.bigIntegerValue()).orMaxUnsignedLong())
      .isEqualTo(UnsignedLong.MAX_VALUE);
    assertThat(FluentBigInteger.of(UnsignedLong.MAX_VALUE.bigIntegerValue().add(BigInteger.ONE)).orMaxUnsignedLong())
      .isEqualTo(UnsignedLong.MAX_VALUE);
  }

  @Test
  public void orMaxUnsignedLongNegative() {
    expectedException.expect(IllegalArgumentException.class);
    FluentBigInteger.of(BigInteger.valueOf(-1L)).orMaxUnsignedLong();
  }

  ///////////
  // minusOrZero
  ///////////

  @Test
  public void minusOrZeroNull() {
    expectedException.expect(NullPointerException.class);
    FluentBigInteger.of(BigInteger.ONE).minusOrZero(null);
  }

  @Test
  public void minusOrZero() {
    assertThat(FluentBigInteger.of(BigInteger.ONE).minusOrZero(BigInteger.ONE).getValue()).isEqualTo(BigInteger.ZERO);
    assertThat(FluentBigInteger.of(BigInteger.ONE).minusOrZero(BigInteger.valueOf(2)).getValue())
      .isEqualTo(BigInteger.ZERO);
    assertThat(FluentBigInteger.of(BigInteger.ZERO).minusOrZero(BigInteger.ONE).getValue()).isEqualTo(BigInteger.ZERO);
    assertThat(FluentBigInteger.of(BigInteger.TEN).minusOrZero(BigInteger.ONE).getValue())
      .isEqualTo(BigInteger.valueOf(9L));
  }


}