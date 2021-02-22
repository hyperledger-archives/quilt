package org.interledger.core.fluent;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.primitives.UnsignedLong;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.math.BigInteger;

/**
 * Unit tests for {@link FluentUnsignedLong}.
 */
public class FluentUnsignedLongTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  ////////////
  // of
  ////////////

  @Test
  public void of() {
    assertThat(FluentUnsignedLong.of(UnsignedLong.MAX_VALUE).getValue()).isEqualTo(UnsignedLong.MAX_VALUE);
  }

  ////////////
  // timesCeilOrZero
  ////////////

  @Test
  public void timesCeilOrZero() {
    final FluentUnsignedLong ful = FluentUnsignedLong.of(UnsignedLong.valueOf(10L));

    Ratio r = Ratio.ZERO;
    assertThat(ful.timesCeilOrZero(r).getValue()).isEqualTo(UnsignedLong.ZERO);

    r = Ratio.ONE;
    assertThat(ful.timesCeilOrZero(r).getValue()).isEqualTo(UnsignedLong.valueOf(10L));

    r = Ratio.builder().numerator(BigInteger.ONE).denominator(BigInteger.ONE).build();
    assertThat(ful.timesCeilOrZero(r).getValue()).isEqualTo(UnsignedLong.valueOf(10L));

    r = Ratio.builder()
      .numerator(BigInteger.valueOf(1L))
      .denominator(BigInteger.valueOf(2L))
      .build();
    assertThat(ful.timesCeilOrZero(r).getValue()).isEqualTo(UnsignedLong.valueOf(5L));

    r = Ratio.builder()
      .numerator(BigInteger.valueOf(3L))
      .denominator(BigInteger.valueOf(2L))
      .build();
    assertThat(ful.timesCeilOrZero(r).getValue()).isEqualTo(UnsignedLong.valueOf(15L));

    r = Ratio.builder()
      .numerator(BigInteger.valueOf(1L))
      .denominator(UnsignedLong.MAX_VALUE.bigIntegerValue())
      .build();
    assertThat(ful.timesCeilOrZero(r).getValue()).isEqualTo(UnsignedLong.ONE);
  }

  @Test
  public void timesCeilOrZeroWithOverflow() {
    UnsignedLong actual = FluentUnsignedLong.of(UnsignedLong.MAX_VALUE)
      .timesCeilOrZero(Ratio.builder()
        .numerator(BigInteger.valueOf(2L))
        .denominator(BigInteger.ONE)
        .build()
      ).getValue();
    assertThat(actual).isEqualTo(UnsignedLong.ZERO);
  }

  ////////////
  // timesFloorOrZero
  ////////////

  @Test
  public void timesFloorOrZero() {
    final FluentUnsignedLong ful = FluentUnsignedLong.of(UnsignedLong.valueOf(10L));

    Ratio r = Ratio.ZERO;
    assertThat(ful.timesFloorOrZero(r).getValue()).isEqualTo(UnsignedLong.ZERO);

    r = Ratio.ONE;
    assertThat(ful.timesFloorOrZero(r).getValue()).isEqualTo(UnsignedLong.valueOf(10L));

    r = Ratio.builder().numerator(BigInteger.ONE).denominator(BigInteger.ONE).build();
    assertThat(ful.timesFloorOrZero(r).getValue()).isEqualTo(UnsignedLong.valueOf(10L));

    r = Ratio.builder()
      .numerator(BigInteger.valueOf(1L))
      .denominator(BigInteger.valueOf(2L))
      .build();
    assertThat(ful.timesFloorOrZero(r).getValue()).isEqualTo(UnsignedLong.valueOf(5L));

    r = Ratio.builder()
      .numerator(BigInteger.valueOf(3L))
      .denominator(BigInteger.valueOf(2L))
      .build();
    assertThat(ful.timesFloorOrZero(r).getValue()).isEqualTo(UnsignedLong.valueOf(15L));

    r = Ratio.builder()
      .numerator(BigInteger.valueOf(1L))
      .denominator(UnsignedLong.MAX_VALUE.bigIntegerValue())
      .build();
    assertThat(ful.timesFloorOrZero(r).getValue()).isEqualTo(UnsignedLong.ZERO);
  }

  @Test
  public void timesFloorOrZeroWithOverflow() {
    UnsignedLong actual = FluentUnsignedLong.of(UnsignedLong.MAX_VALUE)
      .timesFloorOrZero(Ratio.builder()
        .numerator(BigInteger.valueOf(2L))
        .denominator(BigInteger.ONE)
        .build()
      ).getValue();
    assertThat(actual).isEqualTo(UnsignedLong.ZERO);
  }

  ////////////
  // halfCeil
  ////////////

  @Test
  public void halfCeil() {
    assertThat(FluentUnsignedLong.of(UnsignedLong.valueOf(10L)).halfCeil().getValue())
      .isEqualTo(UnsignedLong.valueOf(5L));
    assertThat(FluentUnsignedLong.of(UnsignedLong.valueOf(5L)).halfCeil().getValue())
      .isEqualTo(UnsignedLong.valueOf(3L));
    assertThat(FluentUnsignedLong.of(UnsignedLong.valueOf(1L)).halfCeil().getValue()).isEqualTo(UnsignedLong.ONE);
    assertThat(FluentUnsignedLong.of(UnsignedLong.valueOf(0L)).halfCeil().getValue()).isEqualTo(UnsignedLong.ZERO);
  }

  ////////////
  // orGreater
  ////////////

  @Test
  public void orGreaterNull() {
    expectedException.expect(NullPointerException.class);
    FluentUnsignedLong.of(UnsignedLong.valueOf(10L)).orGreater(null);
  }

  @Test
  public void orGreater() {
    assertThat(FluentUnsignedLong.of(UnsignedLong.valueOf(10L)).orGreater(UnsignedLong.ONE).getValue())
      .isEqualTo(UnsignedLong.valueOf(10L));
    assertThat(FluentUnsignedLong.of(UnsignedLong.valueOf(10L)).orGreater(UnsignedLong.valueOf(11L)).getValue())
      .isEqualTo(UnsignedLong.valueOf(11L));
    assertThat(FluentUnsignedLong.of(UnsignedLong.valueOf(10L)).orGreater(UnsignedLong.valueOf(9L)).getValue())
      .isEqualTo(UnsignedLong.valueOf(10L));
  }

  ////////////
  // orLesser
  ////////////

  @Test
  public void orLesserNull() {
    expectedException.expect(NullPointerException.class);
    FluentUnsignedLong.of(UnsignedLong.valueOf(10L)).orLesser(null);
  }

  @Test
  public void orLesser() {
    assertThat(FluentUnsignedLong.of(UnsignedLong.valueOf(10L)).orLesser(UnsignedLong.ONE).getValue())
      .isEqualTo(UnsignedLong.ONE);
    assertThat(FluentUnsignedLong.of(UnsignedLong.valueOf(10L)).orLesser(UnsignedLong.valueOf(11L)).getValue())
      .isEqualTo(UnsignedLong.valueOf(10L));
    assertThat(FluentUnsignedLong.of(UnsignedLong.valueOf(10L)).orLesser(UnsignedLong.valueOf(9L)).getValue())
      .isEqualTo(UnsignedLong.valueOf(9L));
  }

  ////////////
  // isPositive
  ////////////

  @Test
  public void isPositive() {
    assertThat(FluentUnsignedLong.of(UnsignedLong.valueOf(10L)).isPositive()).isTrue();
    assertThat(FluentUnsignedLong.of(UnsignedLong.valueOf(1L)).isPositive()).isTrue();
    assertThat(FluentUnsignedLong.of(UnsignedLong.valueOf(0L)).isPositive()).isFalse();
  }

  ////////////
  // isNotPositive
  ////////////

  @Test
  public void isNotPositive() {
    assertThat(FluentUnsignedLong.of(UnsignedLong.valueOf(10L)).isNotPositive()).isFalse();
    assertThat(FluentUnsignedLong.of(UnsignedLong.valueOf(1L)).isNotPositive()).isFalse();
    assertThat(FluentUnsignedLong.of(UnsignedLong.valueOf(0L)).isNotPositive()).isTrue();
  }

  ////////////
  // getValue
  ////////////

  @Test
  public void getValue() {
    assertThat(FluentUnsignedLong.of(UnsignedLong.valueOf(10L)).getValue()).isEqualTo(UnsignedLong.valueOf(10L));
  }

  ////////////
  // minusOrZero
  ////////////

  @Test
  public void minusOrZero() {
    assertThat(FluentUnsignedLong.of(UnsignedLong.MAX_VALUE).minusOrZero(UnsignedLong.MAX_VALUE).getValue())
      .isEqualTo(UnsignedLong.ZERO);
    assertThat(FluentUnsignedLong.of(UnsignedLong.valueOf(10L)).minusOrZero(UnsignedLong.valueOf(10L)).getValue())
      .isEqualTo(UnsignedLong.ZERO);
    assertThat(FluentUnsignedLong.of(UnsignedLong.valueOf(10L)).minusOrZero(UnsignedLong.valueOf(9L)).getValue())
      .isEqualTo(UnsignedLong.ONE);
    assertThat(FluentUnsignedLong.of(UnsignedLong.valueOf(10L)).minusOrZero(UnsignedLong.valueOf(20L)).getValue())
      .isEqualTo(UnsignedLong.ZERO);
  }
}