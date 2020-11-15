package org.interledger.core.fluent;


import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

import java.math.BigDecimal;

/**
 * Unit tests for {@link Ratio}.
 */
public class RatioTest {

  @Test
  public void compareToWhenSmaller() {
    Ratio r1 = Ratio.builder().numerator(UnsignedLong.valueOf(1)).denominator(UnsignedLong.valueOf(8))
      .build();
    Ratio r2 = Ratio.builder().numerator(UnsignedLong.valueOf(1))
      .denominator(UnsignedLong.valueOf(4)).build();

    assertThat(r1.compareTo(r2) < 0).isTrue();
    assertThat(r1.compareTo(r2) == 0).isFalse();
    assertThat(r1.compareTo(r2) > 0).isFalse();
  }

  @Test
  public void compareToWhenEqual() {
    Ratio r1 = Ratio.builder().numerator(UnsignedLong.valueOf(1)).denominator(UnsignedLong.valueOf(2))
      .build();
    Ratio r2 = Ratio.builder().numerator(UnsignedLong.valueOf(1)).denominator(UnsignedLong.valueOf(2))
      .build();

    assertThat(r1.compareTo(r2) < 0).isFalse();
    assertThat(r1.compareTo(r2) == 0).isTrue();
    assertThat(r1.compareTo(r2) > 0).isFalse();
  }

  @Test
  public void compareToWhenBigger() {
    Ratio r1 = Ratio.builder().numerator(UnsignedLong.valueOf(1))
      .denominator(UnsignedLong.valueOf(2)).build();
    Ratio r2 = Ratio.builder().numerator(UnsignedLong.valueOf(1)).denominator(UnsignedLong.valueOf(4))
      .build();

    assertThat(r1.compareTo(r2) < 0).isFalse();
    assertThat(r1.compareTo(r2) == 0).isFalse();
    assertThat(r1.compareTo(r2) > 0).isTrue();
  }

  @Test
  public void toBigDecimal() {
    Ratio r1 = Ratio.builder().numerator(UnsignedLong.valueOf(1)).denominator(UnsignedLong.valueOf(2)).build();
    assertThat(r1.toBigDecimal()).isEqualTo(new BigDecimal("0.5000000000"));

    r1 = Ratio.builder().numerator(UnsignedLong.valueOf(2)).denominator(UnsignedLong.valueOf(2)).build();
    assertThat(r1.toBigDecimal()).isEqualTo(new BigDecimal("1.0000000000"));

    r1 = Ratio.builder().numerator(UnsignedLong.valueOf(1)).denominator(UnsignedLong.MAX_VALUE).build();
    assertThat(r1.toBigDecimal()).isEqualTo(new BigDecimal("0e-10"));
  }

  @Test
  public void subtract() {

    Ratio r1 = Ratio.builder().numerator(BigDecimal.valueOf(86806L)).denominator(BigDecimal.valueOf(1000L)).build();
    Ratio r1 = Ratio.builder().numerator(new BigDecimal("8680561804955754")).denominator(100000000000000).build();

  }

}