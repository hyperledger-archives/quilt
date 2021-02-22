package org.interledger.fx;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.interledger.core.fluent.Percentage;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class SlippageTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testPreBuildValues() {
    assertThat(Slippage.NONE.value()).isEqualTo(Percentage.ZERO_PERCENT);
    assertThat(Slippage.ONE_PERCENT.value()).isEqualTo(Percentage.ONE_PERCENT);
  }

  @Test
  public void testBuildWithNegative() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("");

    Slippage.of(Percentage.of(BigDecimal.valueOf(-1L)));
  }

  @Test
  public void testBuildWithMoreThan100() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("");

    Slippage.of(Percentage.of(BigDecimal.valueOf(2.0)));
  }

  @Test
  public void testToString() {
    assertThat(Slippage.NONE.toString()).isEqualTo("Slippage{value=0%}");
    assertThat(Slippage.ONE_PERCENT.toString()).isEqualTo("Slippage{value=1%}");
  }

  @Test
  public void testCompareTo() {
    Slippage Slippage0 = Slippage.NONE;
    Slippage Slippage1 = Slippage.ONE_PERCENT;

    assertThat(Slippage0.compareTo(Slippage0)).isEqualTo(0);
    assertThat(Slippage0.compareTo(Slippage1)).isEqualTo(-1);
    assertThat(Slippage1.compareTo(Slippage0)).isEqualTo(1);
  }

  @Test
  public void testEquals() {
    Slippage Slippage0 = Slippage.NONE;
    Slippage Slippage1 = Slippage.ONE_PERCENT;

    assertThat(Slippage0.equals(Slippage0)).isTrue();
    assertThat(Slippage0.equals(Slippage1)).isFalse();
    assertThat(Slippage1.equals(Slippage0)).isFalse();
  }
}