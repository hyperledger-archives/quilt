package org.interledger.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

/**
 * Unit tests for {@link NumberScalingUtils}.
 */
@RunWith(Parameterized.class)
public class NumberScalingUtilsTest {

  private static final UnsignedLong ONE_HUNDRED = UnsignedLong.valueOf(100L);

  private String description;
  private UnsignedLong sourceAmount;
  private short sourceScale;
  private short destinationScale;
  private UnsignedLong destinationAmount;

  public NumberScalingUtilsTest(
      final String description,
      final UnsignedLong sourceAmount,
      final short sourceScale,
      final short destinationScale,
      final UnsignedLong destinationAmount
  ) {
    this.description = Objects.requireNonNull(description);
    this.sourceAmount = Objects.requireNonNull(sourceAmount);
    this.sourceScale = sourceScale;
    this.destinationScale = destinationScale;
    this.destinationAmount = destinationAmount;
  }

  @Parameterized.Parameters
  @SuppressWarnings("PMD")
  public static Collection<Object[]> testValue() {
    return Arrays.asList(

        /////////////////////
        // USD (scale = 0)
        /////////////////////

        new Object[] {
            "Convert 1 Dollars (scale: 0) to cents (scale: 2)",
            UnsignedLong.ONE, (short) 0, (short) 2,
            ONE_HUNDRED
        },

        // TODO: Finish
      // TODO: Compare with Connector?
        new Object[] {
            "Convert 2 Dollars (scale: 0) to cents (scale: 2)",
            UnsignedLong.valueOf(2), (short) 0, (short) 2,
            UnsignedLong.valueOf(200L)
        }
//
//        new Object[]{
//            "Convert Dollars (scale: 0) to Dollars (scale: 0)",
//            SettlementQuantity.builder().scale(0).amount(BigInteger.ONE).build(),
//            SettlementQuantity.builder().scale(0).amount(BigInteger.ONE).build(),
//        },
//
//        new Object[]{
//            "Convert 99 Cents (scale: 2) to Dollars (scale: 0)",
//            SettlementQuantity.builder().scale(2).amount(BigInteger.valueOf(99)).build(),
//            SettlementQuantity.builder().scale(0).amount(BigInteger.ZERO).build(),
//        },
//
//        new Object[]{
//            "Convert 100 Cents (scale: 2) to Dollars (scale: 0)",
//            SettlementQuantity.builder().scale(2).amount(ONE_HUNDRED).build(),
//            SettlementQuantity.builder().scale(0).amount(BigInteger.ONE).build(),
//        },
//
//        new Object[]{
//            "Convert 101 Cents (scale: 2) to Dollars (scale: 0)",
//            SettlementQuantity.builder().scale(2).amount(BigInteger.valueOf(101)).build(),
//            SettlementQuantity.builder().scale(0).amount(BigInteger.ONE).build(),
//        },
//
//        new Object[]{
//            "Convert 501 Cents (scale: 2) to Dollars (scale: 0)",
//            SettlementQuantity.builder().scale(2).amount(BigInteger.valueOf(501)).build(),
//            SettlementQuantity.builder().scale(0).amount(BigInteger.valueOf(5)).build(),
//        },
//
//        new Object[]{
//            "Convert Dime-Dollars (scale: 1) to Dollars (scale: 0)",
//            SettlementQuantity.builder().scale(1).amount(BigInteger.TEN).build(),
//            SettlementQuantity.builder().scale(0).amount(BigInteger.ONE).build(),
//        },
//
//        new Object[]{
//            "Convert Dime-Dollars (scale: 1) to Milli-Dollars (scale: 6)",
//            SettlementQuantity.builder().scale(1).amount(BigInteger.ONE).build(),
//            SettlementQuantity.builder().scale(6).amount(BigInteger.valueOf(100000L)).build(),
//        },
//
//        ////////////
//        // XRP (scale = 0)
//        ////////////
//
//        new Object[]{
//            "Convert 100 Drops (scale: 6) to Drops (scale: 6)",
//            SettlementQuantity.builder().scale(6).amount(ONE_HUNDRED).build(),
//            SettlementQuantity.builder().scale(6).amount(ONE_HUNDRED).build(),
//        },
//
//        new Object[]{
//            "Convert 1 Drop (scale: 6) to XRP (scale: 0)",
//            SettlementQuantity.builder().scale(6).amount(BigInteger.ONE).build(),
//            SettlementQuantity.builder().scale(0).amount(BigInteger.ZERO).build(),
//        },
//
//        new Object[]{
//            "Convert 100 Drops (scale: 6) to XRP (scale: 0)",
//            SettlementQuantity.builder().scale(6).amount(ONE_HUNDRED).build(),
//            SettlementQuantity.builder().scale(0).amount(BigInteger.ZERO).build(),
//        },
//
//        new Object[]{
//            "Convert 999 Drops (scale: 6) to XRP (scale: 0)",
//            SettlementQuantity.builder().scale(6).amount(ONE_HUNDRED).build(),
//            SettlementQuantity.builder().scale(0).amount(BigInteger.ZERO).build(),
//        },
//
//        new Object[]{
//            "Convert 999999 Drops (scale: 6) to XRP (scale: 0)",
//            SettlementQuantity.builder().scale(6).amount(BigInteger.valueOf(999999L)).build(),
//            SettlementQuantity.builder().scale(0).amount(BigInteger.ZERO).build(),
//        },
//
//        new Object[]{
//            "Convert 1M Drops (scale: 6) to XRP (scale: 0)",
//            SettlementQuantity.builder().scale(6).amount(BigInteger.valueOf(1000000L)).build(),
//            SettlementQuantity.builder().scale(0).amount(BigInteger.ONE).build(),
//        },
//
//        new Object[]{
//            "Convert 1 Milli-Drop (scale: 9) to Drops (scale: 6)",
//            SettlementQuantity.builder().scale(9).amount(BigInteger.ONE).build(),
//            SettlementQuantity.builder().scale(6).amount(BigInteger.ZERO).build(),
//        },
//
//        new Object[]{
//            "Convert 999 Milli-Drops (scale: 9) to Drops (scale: 6)",
//            SettlementQuantity.builder().scale(9).amount(BigInteger.valueOf(999L)).build(),
//            SettlementQuantity.builder().scale(6).amount(BigInteger.ZERO).build(),
//        },
//
//        new Object[]{
//            "Convert 1000 Milli-Drops (scale: 9) to Drops (scale: 6)",
//            SettlementQuantity.builder().scale(9).amount(BigInteger.valueOf(1000L)).build(),
//            SettlementQuantity.builder().scale(6).amount(BigInteger.ONE).build(),
//        },
//
//        //////////////////
//        // Eth (Scale = 0)
//        //////////////////
//
//        new Object[]{
//            "Convert 1 Wei (scale: 18) to Gwei (scale: 9)",
//            SettlementQuantity.builder().scale(18).amount(BigInteger.ONE).build(),
//            SettlementQuantity.builder().scale(9).amount(BigInteger.ZERO).build(),
//        },
//
//        new Object[]{
//            "Convert 1B Wei (scale: 18) to Gwei (scale: 9)",
//            SettlementQuantity.builder().scale(18).amount(BigInteger.valueOf(1000000000L)).build(),
//            SettlementQuantity.builder().scale(9).amount(BigInteger.ONE).build(),
//        },
//
//        new Object[]{
//            "Convert 1B+1 Wei (scale: 18) to Gwei (scale: 9)",
//            SettlementQuantity.builder().scale(18).amount(BigInteger.valueOf(1000000000)).build(),
//            SettlementQuantity.builder().scale(9).amount(BigInteger.ONE).build(),
//        },
//
//        new Object[]{
//            "Convert Wei (scale: 18) to Eth (scale: 0)",
//            SettlementQuantity.builder().scale(18).amount(new BigInteger("1000000000000000000")).build(),
//            SettlementQuantity.builder().scale(0).amount(BigInteger.ONE).build(),
//        },
//
//        new Object[]{
//            "Convert  Wei (scale: 18) to Eth (scale: 0)",
//            SettlementQuantity.builder().scale(9).amount(BigInteger.ONE).build(),
//            SettlementQuantity.builder().scale(18).amount(BigInteger.valueOf(1000000000L)).build(),
//        },
//
//        ////////////////
//        // Rounding Validations
//        // Number scaling utils MUST always round to the floor. The current implementation guarantees this in two ways.
//        // First, if the difference between the destination and source currencies is positive, then the source amount will
//        // be multiplied by (10^diff), which will always yield a whole number. If the difference between the destination
//        // and source currencies is negative, then the source amount will be divided by (10^-diff), which might produce
//        // a remainder. In these scenarios, the implementation will round to the "floor", producing a whole number and
//        // a leftover that can be ignored (and processed later).
//        ////////////////
//
//        new Object[]{
//            "Convert $1.03 to $1 Dollars (rounding down)",
//            SettlementQuantity.builder().scale(2).amount(BigInteger.valueOf(103L)).build(),
//            SettlementQuantity.builder().scale(0).amount(BigInteger.ONE).build(),
//        },
//
//        new Object[]{
//            "Convert $0.99 to $0 Dollars (rounding down)",
//            SettlementQuantity.builder().scale(2).amount(BigInteger.valueOf(99L)).build(),
//            SettlementQuantity.builder().scale(0).amount(BigInteger.ZERO).build(),
//        },
//
//        new Object[]{
//            "Convert 1,000,001 drops to 1 XRP (rounding down)",
//            SettlementQuantity.builder().scale(6).amount(BigInteger.valueOf(1000000)).build(),
//            SettlementQuantity.builder().scale(0).amount(BigInteger.ONE).build(),
//        },
//
//        new Object[]{
//            "Convert 999,999 to $0 XRP (rounding down)",
//            SettlementQuantity.builder().scale(6).amount(BigInteger.valueOf(999999L)).build(),
//            SettlementQuantity.builder().scale(0).amount(BigInteger.ZERO).build(),
//        }

    );
  }

  @Test(expected = NullPointerException.class)
  public void translateWithNullSourceQuantity() {
    try {
      NumberScalingUtils.translate(null, (short) 1, (short) 2);
      fail("Should have thrown an NPE");
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isEqualTo("sourceAmount must not be null");
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void translateWithNegativeSourceScale() {
    try {
      NumberScalingUtils.translate(UnsignedLong.ZERO, (short) -11, (short) 2);
      fail("Should have thrown an NPE");
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isEqualTo("sourceScale must not be negative");
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void translateWithNegativeDestinationScale() {
    try {
      NumberScalingUtils.translate(UnsignedLong.ZERO, (short) 1, (short) -1);
      fail("Should have thrown an NPE");
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isEqualTo("destinationScale must not be negative");
      throw e;
    }
  }

  @Test
  public void translate() {
    assertThat(NumberScalingUtils.translate(sourceAmount, sourceScale, destinationScale)).as(description)
        .isEqualTo(destinationAmount);
  }
}
