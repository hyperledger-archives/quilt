package org.interledger.ilqp;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.interledger.InterledgerAddress;

import org.junit.Test;

import java.math.BigInteger;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Unit tests for {@link QuoteByDestinationAmountRequest}.
 */
public class QuoteByDestinationAmountRequestTest {

  private static final InterledgerAddress destinationAccount = InterledgerAddress
      .of("test1.foo.quote");
  private static final BigInteger destinationAmount = BigInteger.TEN;
  private static final Duration destinationHoldDuration = Duration.ZERO;

  @Test
  public void testBuild() throws Exception {
    final QuoteByDestinationAmountRequest quoteRequest =
        QuoteByDestinationAmountRequest.builder()
            .destinationAccount(destinationAccount)
            .destinationAmount(destinationAmount)
            .destinationHoldDuration(destinationHoldDuration).build();

    assertThat(quoteRequest.getDestinationAccount(), is(destinationAccount));
    assertThat(quoteRequest.getDestinationAmount(), is(destinationAmount));
    assertThat(quoteRequest.getDestinationHoldDuration(), is(destinationHoldDuration));
  }

  @Test
  public void testZeroAmount() throws Exception {
    final QuoteByDestinationAmountRequest quoteRequest =
        QuoteByDestinationAmountRequest.builder()
            .destinationAccount(destinationAccount)
            .destinationAmount(BigInteger.ZERO)
            .destinationHoldDuration(destinationHoldDuration).build();

    assertThat(quoteRequest.getDestinationAccount(), is(destinationAccount));
    assertThat(quoteRequest.getDestinationAmount(), is(BigInteger.ZERO));
    assertThat(quoteRequest.getDestinationHoldDuration(), is(destinationHoldDuration));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNegativeAmount() throws Exception {
    try {
      QuoteByDestinationAmountRequest.builder()
          .destinationAccount(destinationAccount)
          .destinationAmount(BigInteger.valueOf(-10L))
          .destinationHoldDuration(destinationHoldDuration).build();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("destinationAmount must be at least 0!"));
      throw e;
    }
  }

  @Test
  public void testBuildWithNullValues() throws Exception {
    try {
      QuoteByDestinationAmountRequest.builder().build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("destinationAccount must not be null!"));
    }

    try {
      QuoteByDestinationAmountRequest.builder().destinationAccount(destinationAccount).build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("destinationAmount must not be null!"));
    }

    try {
      QuoteByDestinationAmountRequest.builder()
          .destinationAccount(destinationAccount)
          .destinationAmount(destinationAmount)
          .build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("destinationHoldDuration must not be null!"));
    }
  }

  @Test
  public void testEqualsHashCode() throws Exception {
    final QuoteByDestinationAmountRequest quoteRequest1 =
        QuoteByDestinationAmountRequest.builder()
            .destinationAccount(destinationAccount)
            .destinationAmount(destinationAmount)
            .destinationHoldDuration(destinationHoldDuration)
            .build();

    final QuoteByDestinationAmountRequest quoteRequest2 =
        QuoteByDestinationAmountRequest.builder()
            .destinationAccount(destinationAccount)
            .destinationAmount(destinationAmount)
            .destinationHoldDuration(destinationHoldDuration)
            .build();

    assertTrue(quoteRequest1.equals(quoteRequest2));
    assertTrue(quoteRequest2.equals(quoteRequest1));
    assertTrue(quoteRequest1.hashCode() == quoteRequest2.hashCode());

    {
      final QuoteByDestinationAmountRequest quoteRequest3 = QuoteByDestinationAmountRequest
          .builder()
          .destinationAccount(destinationAccount)
          .destinationAmount(destinationAmount)
          .destinationHoldDuration(Duration.of(1L, ChronoUnit.SECONDS))
          .build();

      assertFalse(quoteRequest1.equals(quoteRequest3));
      assertFalse(quoteRequest3.equals(quoteRequest1));
      assertFalse(quoteRequest1.hashCode() == quoteRequest3.hashCode());
    }

    {
      final QuoteByDestinationAmountRequest quoteRequest4 = QuoteByDestinationAmountRequest
          .builder()
          .destinationAccount(destinationAccount)
          .destinationAmount(BigInteger.ONE)
          .destinationHoldDuration(destinationHoldDuration)
          .build();

      assertFalse(quoteRequest1.equals(quoteRequest4));
      assertFalse(quoteRequest4.equals(quoteRequest1));
      assertFalse(quoteRequest1.hashCode() == quoteRequest4.hashCode());
    }

    {
      final QuoteByDestinationAmountRequest quoteRequest5 = QuoteByDestinationAmountRequest
          .builder()
          .destinationAccount(InterledgerAddress.of("self.foo"))
          .destinationAmount(destinationAmount)
          .destinationHoldDuration(destinationHoldDuration)
          .build();

      assertFalse(quoteRequest1.equals(quoteRequest5));
      assertFalse(quoteRequest5.equals(quoteRequest1));
      assertFalse(quoteRequest1.hashCode() == quoteRequest5.hashCode());
    }
  }

}