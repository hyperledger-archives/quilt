package org.interledger.ilqp;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.math.BigInteger;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Unit tests for {@link QuoteByDestinationAmountResponse}.
 */
public class QuoteByDestinationAmountResponseTest {

  private static final BigInteger sourceAmount = BigInteger.TEN;
  private static final Duration sourceHoldDuration = Duration.ZERO;

  @Test
  public void testBuild() throws Exception {
    final QuoteByDestinationAmountResponse quoteResponse =
        QuoteByDestinationAmountResponse.builder()
            .sourceAmount(sourceAmount)
            .sourceHoldDuration(sourceHoldDuration).build();

    assertThat(quoteResponse.getSourceAmount(), is(sourceAmount));
    assertThat(quoteResponse.getSourceHoldDuration(), is(sourceHoldDuration));
  }

  @Test
  public void testZeroAmount() throws Exception {
    final QuoteByDestinationAmountResponse quoteRequest =
        QuoteByDestinationAmountResponse.builder()
            .sourceAmount(BigInteger.ZERO)
            .sourceHoldDuration(sourceHoldDuration).build();

    assertThat(quoteRequest.getSourceAmount(), is(BigInteger.ZERO));
    assertThat(quoteRequest.getSourceHoldDuration(), is(sourceHoldDuration));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNegativeAmount() throws Exception {
    try {
      QuoteByDestinationAmountResponse.builder()
          .sourceAmount(BigInteger.valueOf(-11L))
          .sourceHoldDuration(sourceHoldDuration).build();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("destinationAmount must be at least 0!"));
      throw e;
    }
  }

  @Test
  public void testBuildWithNullValues() throws Exception {
    try {
      QuoteByDestinationAmountResponse.builder().build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("sourceAmount must not be null!"));
    }

    try {
      QuoteByDestinationAmountResponse.builder()
          .sourceAmount(sourceAmount)
          .build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("sourceHoldDuration must not be null!"));
    }
  }

  @Test
  public void testEqualsHashCode() throws Exception {
    final QuoteByDestinationAmountResponse quoteResponse1 =
        QuoteByDestinationAmountResponse.builder()
            .sourceAmount(sourceAmount)
            .sourceHoldDuration(sourceHoldDuration)
            .build();

    final QuoteByDestinationAmountResponse quoteResponse2 =
        QuoteByDestinationAmountResponse.builder()
            .sourceAmount(sourceAmount)
            .sourceHoldDuration(sourceHoldDuration)
            .build();

    assertTrue(quoteResponse1.equals(quoteResponse2));
    assertTrue(quoteResponse2.equals(quoteResponse1));
    assertTrue(quoteResponse1.hashCode() == quoteResponse2.hashCode());

    {
      final QuoteByDestinationAmountResponse quoteResponse3 = QuoteByDestinationAmountResponse
          .builder()
          .sourceAmount(sourceAmount)
          .sourceHoldDuration(Duration.of(1L, ChronoUnit.SECONDS))
          .build();

      assertFalse(quoteResponse1.equals(quoteResponse3));
      assertFalse(quoteResponse3.equals(quoteResponse1));
      assertFalse(quoteResponse1.hashCode() == quoteResponse3.hashCode());
    }

    {
      final QuoteByDestinationAmountResponse quoteResponse4 = QuoteByDestinationAmountResponse
          .builder()
          .sourceAmount(BigInteger.ONE)
          .sourceHoldDuration(sourceHoldDuration)
          .build();

      assertFalse(quoteResponse1.equals(quoteResponse4));
      assertFalse(quoteResponse4.equals(quoteResponse1));
      assertFalse(quoteResponse1.hashCode() == quoteResponse4.hashCode());
    }
  }

}