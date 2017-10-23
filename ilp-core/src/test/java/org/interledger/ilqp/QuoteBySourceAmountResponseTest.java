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
 * Unit tests for {@link QuoteBySourceAmountResponse}.
 */
public class QuoteBySourceAmountResponseTest {

  private static final BigInteger destinationAmount = BigInteger.TEN;
  private static final Duration sourceHoldDuration = Duration.ZERO;

  @Test
  public void testBuild() throws Exception {
    final QuoteBySourceAmountResponse quoteResponse =
        QuoteBySourceAmountResponse.builder()
            .destinationAmount(destinationAmount)
            .sourceHoldDuration(sourceHoldDuration).build();

    assertThat(quoteResponse.getDestinationAmount(), is(destinationAmount));
    assertThat(quoteResponse.getSourceHoldDuration(), is(sourceHoldDuration));
  }


  @Test
  public void testZeroAmount() throws Exception {
    final QuoteBySourceAmountResponse quoteRequest =
        QuoteBySourceAmountResponse.builder()
            .destinationAmount(BigInteger.ZERO)
            .sourceHoldDuration(sourceHoldDuration).build();

    assertThat(quoteRequest.getDestinationAmount(), is(BigInteger.ZERO));
    assertThat(quoteRequest.getSourceHoldDuration(), is(sourceHoldDuration));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNegativeAmount() throws Exception {
    try {
      QuoteBySourceAmountResponse.builder()
          .destinationAmount(BigInteger.valueOf(-11L))
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
      QuoteBySourceAmountResponse.builder().build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("destinationAmount must not be null!"));
    }

    try {
      QuoteBySourceAmountResponse.builder()
          .destinationAmount(destinationAmount)
          .build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("sourceHoldDuration must not be null!"));
    }
  }

  @Test
  public void testEqualsHashCode() throws Exception {
    final QuoteBySourceAmountResponse quoteRequest1 =
        QuoteBySourceAmountResponse.builder()
            .destinationAmount(destinationAmount)
            .sourceHoldDuration(sourceHoldDuration)
            .build();

    final QuoteBySourceAmountResponse quoteRequest2 =
        QuoteBySourceAmountResponse.builder()
            .destinationAmount(destinationAmount)
            .sourceHoldDuration(sourceHoldDuration)
            .build();

    assertTrue(quoteRequest1.equals(quoteRequest2));
    assertTrue(quoteRequest2.equals(quoteRequest1));
    assertTrue(quoteRequest1.hashCode() == quoteRequest2.hashCode());

    {
      final QuoteBySourceAmountResponse quoteRequest3 = QuoteBySourceAmountResponse
          .builder()
          .destinationAmount(destinationAmount)
          .sourceHoldDuration(Duration.of(1L, ChronoUnit.SECONDS))
          .build();

      assertFalse(quoteRequest1.equals(quoteRequest3));
      assertFalse(quoteRequest3.equals(quoteRequest1));
      assertFalse(quoteRequest1.hashCode() == quoteRequest3.hashCode());
    }

    {
      final QuoteBySourceAmountResponse quoteRequest4 = QuoteBySourceAmountResponse
          .builder()
          .destinationAmount(BigInteger.ONE)
          .sourceHoldDuration(sourceHoldDuration)
          .build();

      assertFalse(quoteRequest1.equals(quoteRequest4));
      assertFalse(quoteRequest4.equals(quoteRequest1));
      assertFalse(quoteRequest1.hashCode() == quoteRequest4.hashCode());
    }
  }
}