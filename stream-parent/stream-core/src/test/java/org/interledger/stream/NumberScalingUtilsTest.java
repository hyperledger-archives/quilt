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

  /**
   * Required-args Constructor.
   *
   * @param description       A {@link String}.
   * @param sourceScale       A short.
   * @param sourceAmount      An {@link UnsignedLong}.
   * @param destinationScale  A short.
   * @param destinationAmount An {@link UnsignedLong}.
   */
  public NumberScalingUtilsTest(
    final String description,
    final short sourceScale,
    final UnsignedLong sourceAmount,
    final short destinationScale,
    final UnsignedLong destinationAmount
  ) {
    this.description = Objects.requireNonNull(description);
    this.sourceScale = sourceScale;
    this.sourceAmount = Objects.requireNonNull(sourceAmount);
    this.destinationScale = destinationScale;
    this.destinationAmount = Objects.requireNonNull(destinationAmount);
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
        (short) 0, UnsignedLong.ONE,
        (short) 2, ONE_HUNDRED
      },

      new Object[] {
        "Convert 2 Dollars (scale: 0) to cents (scale: 2)",
        (short) 0, UnsignedLong.valueOf(2),
        (short) 2, UnsignedLong.valueOf(200L)
      },

      new Object[] {
        "Convert Dollars (scale: 0) to Dollars (scale: 0)",
        (short) 0, UnsignedLong.ONE,
        (short) 0, UnsignedLong.ONE
      },

      new Object[] {
        "Convert 99 Cents (scale: 2) to Dollars (scale: 0)",
        (short) 2, UnsignedLong.valueOf(99),
        (short) 0, UnsignedLong.ZERO
      },

      new Object[] {
        "Convert 100 Cents (scale: 2) to Dollars (scale: 0)",
        (short) 2, ONE_HUNDRED,
        (short) 0, UnsignedLong.ONE
      },

      new Object[] {
        "Convert 101 Cents (scale: 2) to Dollars (scale: 0)",
        (short) 2, UnsignedLong.valueOf(101),
        (short) 0, UnsignedLong.ONE
      },

      new Object[] {
        "Convert 501 Cents (scale: 2) to Dollars (scale: 0)",
        (short) 2, UnsignedLong.valueOf(501),
        (short) 0, UnsignedLong.valueOf(5)
      },

      new Object[] {
        "Convert Dime-Dollars (scale: 1) to Dollars (scale: 0)",
        (short) 1, UnsignedLong.valueOf(10),
        (short) 0, UnsignedLong.ONE
      },

      new Object[] {
        "Convert Dime-Dollars (scale: 1) to Milli-Dollars (scale: 6)",
        (short) 1, UnsignedLong.ONE,
        (short) 6, UnsignedLong.valueOf(100000L)
      },

      ////////////
      // XRP (scale = 0)
      ////////////

      new Object[] {
        "Convert 100 Drops (scale: 6) to Drops (scale: 6)",
        (short) 6, ONE_HUNDRED,
        (short) 6, ONE_HUNDRED
      },

      new Object[] {
        "Convert 1 Drop (scale: 6) to XRP (scale: 0)",
        (short) 6, UnsignedLong.ONE,
        (short) 0, UnsignedLong.ZERO
      },

      new Object[] {
        "Convert 100 Drops (scale: 6) to XRP (scale: 0)",
        (short) 6, ONE_HUNDRED,
        (short) 0, UnsignedLong.ZERO
      },

      new Object[] {
        "Convert 999 Drops (scale: 6) to XRP (scale: 0)",
        (short) 6, UnsignedLong.valueOf(999L),
        (short) 0, UnsignedLong.ZERO
      },

      new Object[] {
        "Convert 999999 Drops (scale: 6) to XRP (scale: 0)",
        (short) 6, UnsignedLong.valueOf(999999L),
        (short) 0, UnsignedLong.ZERO
      },

      new Object[] {
        "Convert 1M Drops (scale: 6) to XRP (scale: 0)",
        (short) 6, UnsignedLong.valueOf(1000000L),
        (short) 0, UnsignedLong.ONE
      },

      new Object[] {
        "Convert 1 Milli-Drop (scale: 9) to Drops (scale: 6)",
        (short) 9, UnsignedLong.ONE,
        (short) 6, UnsignedLong.ZERO
      },

      new Object[] {
        "Convert 999 Milli-Drops (scale: 9) to Drops (scale: 6)",
        (short) 9, UnsignedLong.valueOf(999L),
        (short) 6, UnsignedLong.ZERO
      },

      new Object[] {
        "Convert 1000 Milli-Drops (scale: 9) to Drops (scale: 6)",
        (short) 9, UnsignedLong.valueOf(1000L),
        (short) 6, UnsignedLong.ONE
      },

      //////////////////
      // Eth (Scale = 0)
      //////////////////

      new Object[] {
        "Convert 1 Wei (scale: 18) to Gwei (scale: 9)",
        (short) 18, UnsignedLong.ONE,
        (short) 9, UnsignedLong.ZERO
      },

      new Object[] {
        "Convert 1B Wei (scale: 18) to Gwei (scale: 9)",
        (short) 18, UnsignedLong.valueOf(1000000000L),
        (short) 9, UnsignedLong.ONE
      },

      new Object[] {
        "Convert 1B+1 Wei (scale: 18) to Gwei (scale: 9)",
        (short) 18, UnsignedLong.valueOf(1000000001L),
        (short) 9, UnsignedLong.ONE
      },

      new Object[] {
        "Convert Wei (scale: 18) to Eth (scale: 0)",
        (short) 18, UnsignedLong.valueOf(1000000000000000000L),
        (short) 0, UnsignedLong.ONE
      },

      new Object[] {
        "Convert  Eth (scale: 9) to Wei (scale: 18)",
        (short) 9, UnsignedLong.ONE,
        (short) 18, UnsignedLong.valueOf(1000000000L)
      },

      ////////////////
      // Rounding Validations
      // Number scaling utils MUST always round to the floor. The current implementation guarantees this in two ways.
      // First, if the difference between the destination and source currencies is positive, then the source amount will
      // be multiplied by (10^diff), which will always yield a whole number. If the difference between the destination
      // and source currencies is negative, then the source amount will be divided by (10^-diff), which might produce
      // a remainder. In these scenarios, the implementation will round to the "floor", producing a whole number and
      // a leftover that can be ignored (and processed later).
      ////////////////

      new Object[] {
        "Convert $1.03 to $1 Dollars (rounding down)",
        (short) 2, UnsignedLong.valueOf(103L),
        (short) 0, UnsignedLong.valueOf(1)
      },

      new Object[] {
        "Convert $0.99 to $0 Dollars (rounding down)",
        (short) 2, UnsignedLong.valueOf(99L),
        (short) 0, UnsignedLong.ZERO
      },

      new Object[] {
        "Convert 1,000,001 drops to 1 XRP (rounding down)",
        (short) 6, UnsignedLong.valueOf(1000001L),
        (short) 0, UnsignedLong.ONE
      },

      new Object[] {
        "Convert 999,999 to $0 XRP (rounding down)",
        (short) 6, UnsignedLong.valueOf(999999L),
        (short) 0, UnsignedLong.ZERO
      }

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
