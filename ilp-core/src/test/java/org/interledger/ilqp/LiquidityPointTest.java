package org.interledger.ilqp;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.math.BigInteger;

/**
 * Unit tests for {@link LiquidityPoint}.
 */
public class LiquidityPointTest {

  @Test
  public void testBuild() throws Exception {
    final LiquidityPoint liquidityPoint =
        LiquidityPoint.builder()
            .inputAmount(BigInteger.ONE)
            .outputAmount(BigInteger.TEN)
            .build();

    assertThat(liquidityPoint.getInputAmount(), is(BigInteger.ONE));
    assertThat(liquidityPoint.getOutputAmount(), is(BigInteger.TEN));
  }

  @Test
  public void testBuildWithNullValues() throws Exception {
    try {
      LiquidityPoint.builder().build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("inputAmount must not be null!"));
    }

    try {
      LiquidityPoint.builder().inputAmount(BigInteger.ZERO).build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("outputAmount must not be null!"));
    }

    try {
      LiquidityPoint.builder().outputAmount(BigInteger.ZERO).build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("inputAmount must not be null!"));
    }
  }

  @Test
  public void testEqualsHashCode() throws Exception {
    final LiquidityPoint liquidityPoint1 =
        LiquidityPoint.builder()
            .inputAmount(BigInteger.ZERO)
            .outputAmount(BigInteger.ONE)
            .build();

    final LiquidityPoint liquidityPoint2 =
        LiquidityPoint.builder()
            .inputAmount(BigInteger.ZERO)
            .outputAmount(BigInteger.ONE)
            .build();

    assertTrue(liquidityPoint1.equals(liquidityPoint2));
    assertTrue(liquidityPoint2.equals(liquidityPoint1));
    assertTrue(liquidityPoint1.hashCode() == liquidityPoint2.hashCode());

    {
      final LiquidityPoint liquidityPoint3 =
          LiquidityPoint.builder()
              .inputAmount(BigInteger.TEN)
              .outputAmount(BigInteger.TEN)
              .build();

      assertFalse(liquidityPoint1.equals(liquidityPoint3));
      assertFalse(liquidityPoint3.equals(liquidityPoint1));
      assertFalse(liquidityPoint1.hashCode() == liquidityPoint3.hashCode());
    }

  }

}