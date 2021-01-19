package org.interledger.core.fluent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.math.BigDecimal;

/**
 * Unit tests for {@link Percentage}.
 */
public class PercentageTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testPreBuildValues() {
    assertThat(Percentage.ZERO_PERCENT.value()).isEqualTo(BigDecimal.ZERO);
    assertThat(Percentage.ONE_PERCENT.value()).isEqualTo(new BigDecimal("0.01"));
    assertThat(Percentage.FIVE_PERCENT.value()).isEqualTo(new BigDecimal("0.05"));
    assertThat(Percentage.FIFTY_PERCENT.value()).isEqualTo(new BigDecimal("0.5"));
    assertThat(Percentage.ONE_HUNDRED_PERCENT.value()).isEqualTo(BigDecimal.ONE);
  }

  @Test
  public void testBuildWithNegative() {
    Percentage.of(BigDecimal.valueOf(-1L));
  }

  @Test
  public void testBuildWithMoreThan100() {
    Percentage percentage = Percentage.of(BigDecimal.valueOf(2L));
    assertThat(percentage.value()).isEqualTo(new BigDecimal("2"));
    assertThat(percentage.toString()).isEqualTo("200%");
  }

  @Test
  public void testToString() {
    assertThat(Percentage.ZERO_PERCENT.toString()).isEqualTo("0%");
    assertThat(Percentage.ONE_PERCENT.toString()).isEqualTo("1%");
    assertThat(Percentage.FIVE_PERCENT.toString()).isEqualTo("5%");
    assertThat(Percentage.FIFTY_PERCENT.toString()).isEqualTo("50%");
    assertThat(Percentage.ONE_HUNDRED_PERCENT.toString()).isEqualTo("100%");
    assertThat(Percentage.of(new BigDecimal("0.012555")).toString()).isEqualTo("1.2555%");
  }

  @Test
  public void testCompareTo() {
    Percentage percentage0 = Percentage.ZERO_PERCENT;
    Percentage percentage0b = Percentage.of(new BigDecimal("0.0000000"));

    assertThat(percentage0.compareTo(percentage0)).isEqualTo(0);
    assertThat(percentage0.compareTo(percentage0b)).isEqualTo(0);
    assertThat(percentage0b.compareTo(percentage0)).isEqualTo(0);
  }

  @Test
  public void testEquals() {
    Percentage percentage0 = Percentage.ZERO_PERCENT;
    Percentage percentage100 = Percentage.ONE_HUNDRED_PERCENT;

    assertThat(percentage0.equals(percentage0)).isTrue();
    assertThat(percentage0.equals(percentage100)).isFalse();
    assertThat(percentage100.equals(percentage0)).isFalse();
  }

  @Test
  public void testEqualsWithDifferentUnderlying0() {
    Percentage percentage0 = Percentage.ZERO_PERCENT;
    Percentage percentage0b = Percentage.of(new BigDecimal("0.0000000"));

    assertThat(percentage0.equals(percentage0)).isTrue();
    assertThat(percentage0.equals(percentage0b)).isFalse();
    assertThat(percentage0b.equals(percentage0)).isFalse();
  }

  @Test
  public void testEqualsWithDifferentUnderlying100() {
    Percentage percentage0 = Percentage.ONE_HUNDRED_PERCENT;
    Percentage percentage0b = Percentage.of(new BigDecimal("1.0"));

    assertThat(percentage0.equals(percentage0)).isTrue();
    assertThat(percentage0.equals(percentage0b)).isFalse();
    assertThat(percentage0b.equals(percentage0)).isFalse();
  }

}