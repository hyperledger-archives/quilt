package org.interledger.ilp;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import org.interledger.InterledgerAddress;
import org.interledger.ilp.InterledgerPayment.Builder;

import org.junit.Test;

import java.math.BigInteger;

/**
 * Unit tests for {@link InterledgerPayment} and {@link InterledgerPayment.Builder}.
 */
public class InterledgerPaymentTest {

  @Test
  public void testBuild() throws Exception {
    final InterledgerAddress destinationAccount = mock(InterledgerAddress.class);
    final BigInteger destinationAmount = BigInteger.valueOf(25L);
    byte[] data = new byte[]{127};

    final InterledgerPayment interledgerPayment =
        InterledgerPayment.builder().destinationAccount(destinationAccount)
            .destinationAmount(destinationAmount).data(data).build();

    assertThat(interledgerPayment.getDestinationAccount(), is(destinationAccount));
    assertThat(interledgerPayment.getDestinationAmount(), is(destinationAmount));
    assertThat(interledgerPayment.getData(), is(data));
  }

  @Test
  public void testBuildWithNullValues() throws Exception {
    try {
      new Builder().build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("destinationAccount must not be null!"));
    }

    try {
      new Builder().destinationAccount(mock(InterledgerAddress.class)).build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("destinationAmount must not be null!"));
    }

    try {
      new Builder().destinationAccount(mock(InterledgerAddress.class))
          .destinationAmount(BigInteger.valueOf(100L))
          .build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("data must not be null!"));
    }

    final InterledgerPayment interledgerPayment =
        new Builder().destinationAccount(mock(InterledgerAddress.class))
            .destinationAmount(BigInteger.valueOf(100L))
            .data(new byte[]{}).build();
    assertThat(interledgerPayment, is(not(nullValue())));
  }

  @Test
  public void testEqualsHashCode() throws Exception {
    final InterledgerAddress destinationAccount = mock(InterledgerAddress.class);
    byte[] data = new byte[]{127};

    final InterledgerPayment interledgerPayment1 =
        new Builder().destinationAccount(destinationAccount)
            .destinationAmount(BigInteger.valueOf(100L))
            .data(data).build();

    final InterledgerPayment interledgerPayment2 =
        new Builder().destinationAccount(destinationAccount)
            .destinationAmount(BigInteger.valueOf(100L))
            .data(data).build();

    assertTrue(interledgerPayment1.equals(interledgerPayment2));
    assertTrue(interledgerPayment2.equals(interledgerPayment1));
    assertTrue(interledgerPayment1.hashCode() == interledgerPayment2.hashCode());

    final InterledgerPayment interledgerPaymentOther = new Builder()
        .destinationAccount(destinationAccount).destinationAmount(BigInteger.valueOf(10L))
        .data(data).build();

    assertFalse(interledgerPayment1.equals(interledgerPaymentOther));
    assertFalse(interledgerPaymentOther.equals(interledgerPayment1));
    assertFalse(interledgerPayment1.hashCode() == interledgerPaymentOther.hashCode());
  }

}
