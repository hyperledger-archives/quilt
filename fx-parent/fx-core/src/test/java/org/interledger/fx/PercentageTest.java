package org.interledger.fx;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit tests for {@link Percentage}.
 */
public class PercentageTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testPreBuildValues() {
    assertThat(Percentage.ZERO_PERCENT.value()).isEqualTo(BigDecimal.ZERO.setScale(4));
    assertThat(Percentage.ONE_PERCENT.value()).isEqualTo(new BigDecimal(0.01).setScale(4, RoundingMode.HALF_UP));
    assertThat(Percentage.FIFTY_PERCENT.value()).isEqualTo(new BigDecimal(0.5).setScale(4, RoundingMode.HALF_UP));
    assertThat(Percentage.ONE_HUNDRED_PERCENT.value()).isEqualTo(new BigDecimal(1).setScale(4, RoundingMode.HALF_UP));
  }

  @Test
  public void testBuildWithNegative() {
    Percentage.of(BigDecimal.valueOf(-1L));
  }

  @Test
  public void testBuildWithMoreThan100() {
    Percentage percentage = Percentage.of(BigDecimal.valueOf(2L));
    assertThat(percentage.value()).isEqualTo(new BigDecimal("2.0000"));
  }

  @Test
  public void testToString() {
    assertThat(Percentage.ZERO_PERCENT.toString()).isEqualTo("0.00%");
    assertThat(Percentage.ONE_PERCENT.toString()).isEqualTo("1.00%");
    assertThat(Percentage.FIFTY_PERCENT.toString()).isEqualTo("50.00%");
    assertThat(Percentage.ONE_HUNDRED_PERCENT.toString()).isEqualTo("100.00%");
    assertThat(Percentage.of(new BigDecimal("0.012555")).toString()).isEqualTo("1.26%");
  }

  @Test
  public void testCompareTo() {
    Percentage percentage0 = Percentage.ZERO_PERCENT;
    Percentage percentage100 = Percentage.ONE_HUNDRED_PERCENT;

    assertThat(percentage0.compareTo(percentage0)).isEqualTo(0);
    assertThat(percentage0.compareTo(percentage100)).isEqualTo(-1);
    assertThat(percentage100.compareTo(percentage0)).isEqualTo(1);
  }

  @Test
  public void testEquals() {
    Percentage percentage0 = Percentage.ZERO_PERCENT;
    Percentage percentage100 = Percentage.ONE_HUNDRED_PERCENT;

    assertThat(percentage0.equals(percentage0)).isTrue();
    assertThat(percentage0.equals(percentage100)).isFalse();
    assertThat(percentage100.equals(percentage0)).isFalse();
  }

}