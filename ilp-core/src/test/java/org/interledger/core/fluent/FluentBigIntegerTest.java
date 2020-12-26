package org.interledger.core.fluent;


import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

@RunWith(Parameterized.class)
public class FluentBigIntegerTest {

  private final Ratio marginOfError;
  private final BigInteger expectedMinPacketAmount;

  public FluentBigIntegerTest(final Ratio marginOfError, final BigInteger expectedMinPacketAmount) {
    this.marginOfError = Objects.requireNonNull(marginOfError);
    this.expectedMinPacketAmount = Objects.requireNonNull(expectedMinPacketAmount);
  }

  /**
   * The data for this test. Note that in some cases the input and output format differ, because we will always include
   * a full time portion. we therefore need input, expected value, and expected output data to properly test.
   */
  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {

      // The smaller the margin, the larger the minPacketAmount will be. Any margin greater than 1 will yield a
      // minPacket of 1.

      /*
       * test parameters are arrays of the form:
       * [Ratio] [BigInteger]
       */
      {
        Ratio.from(new BigDecimal("0.2500001").subtract(new BigDecimal("0.2500000"))), // marginOfError
        BigInteger.valueOf(10000000L)                // minPacketAmount
      },
      {
        Ratio.from(new BigDecimal("0.0001")), // marginOfError
        BigInteger.valueOf(10000L)                // minPacketAmount
      },
      {
        Ratio.from(new BigDecimal("0.001")), // marginOfError
        BigInteger.valueOf(1000L)                   // minPacketAmount
      },
      {
        Ratio.from(new BigDecimal("0.333")), // marginOfError
        BigInteger.valueOf(4L)                   // minPacketAmount
      },
      {
        Ratio.from(new BigDecimal("0.999")), // marginOfError
        BigInteger.valueOf(2L)                   // minPacketAmount
      },
      {
        Ratio.from(new BigDecimal("1.0")), // marginOfError
        BigInteger.valueOf(1L)                 // minPacketAmount
      },
      {
        Ratio.from(new BigDecimal("1.9")), // marginOfError
        BigInteger.valueOf(1L)                 // minPacketAmount
      },
      {
        Ratio.from(new BigDecimal("10.9")), // marginOfError
        BigInteger.valueOf(1L)                  // minPacketAmount
      },
    });
  }

  @Test
  public void timesCeil() {
    BigInteger actual = FluentBigInteger.of(BigInteger.ONE)
      .timesCeil(this.marginOfError.reciprocal().get())
      .getValue();
    assertThat(actual).isEqualTo(this.expectedMinPacketAmount);
  }
}