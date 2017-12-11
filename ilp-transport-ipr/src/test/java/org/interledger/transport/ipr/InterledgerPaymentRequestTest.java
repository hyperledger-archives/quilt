package org.interledger.transport.ipr;

import org.interledger.InterledgerAddress;
import org.interledger.codecs.InterledgerCodecContext;
import org.interledger.codecs.InterledgerCodecContextFactory;
import org.interledger.cryptoconditions.Condition;
import org.interledger.cryptoconditions.PreimageSha256Condition;
import org.interledger.ilp.InterledgerPayment;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class InterledgerPaymentRequestTest {

  // first data value (0) is default
  @Parameter
  public InterledgerPaymentRequest ipr;

  @Parameter(1)
  public InterledgerPayment payment;

  @Parameter(2)
  public Condition condition;

  /**
   * The data for this test...
   */
  @Parameters
  public static Collection<Object[]> data() throws IOException {

    InterledgerPayment payment1 =
        InterledgerPayment.builder().destinationAccount(InterledgerAddress.of("test1.foo"))
            .destinationAmount(BigInteger.valueOf(100L)).data("test data".getBytes()).build();

    Condition condition1 = new PreimageSha256Condition(32, new byte[32]);

    InterledgerPaymentRequest ipr1 =
        InterledgerPaymentRequest.builder()
            .interledgerPayment(payment1).condition(condition1).build();

    InterledgerPayment payment2 =
        InterledgerPayment.builder().destinationAccount(InterledgerAddress.of("test2.bar"))
            .destinationAmount(BigInteger.ZERO).data("other data".getBytes()).build();

    Condition condition2 = new PreimageSha256Condition(32,
        new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 0,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 0,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2});

    InterledgerPaymentRequest ipr2 =
        InterledgerPaymentRequest.builder()
            .interledgerPayment(payment2).condition(condition2).build();

    return Arrays
        .asList(new Object[][]{{ipr1, payment1, condition1}, {ipr2, payment2, condition2},});
  }

  @Test
  public void testWriteRead() throws Exception {

    final InterledgerCodecContext context = InterledgerCodecContextFactory.oer();

    // Write the payment to ASN.1...
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    context.write(ipr, outputStream);

    // Read the bytes using Codecs...
    final ByteArrayInputStream asn1OerPaymentBytes =
        new ByteArrayInputStream(outputStream.toByteArray());

    final InterledgerPaymentRequest decodedIpr =
        context.read(InterledgerPaymentRequest.class, asn1OerPaymentBytes);

    final InterledgerPayment decodedPayment = decodedIpr.getInterledgerPayment();
    final Condition decodedCondition = decodedIpr.getCondition();

    // Validate the PSK Info....
    MatcherAssert.assertThat(decodedPayment.getDestinationAccount(),
        CoreMatchers.is(payment.getDestinationAccount()));
    MatcherAssert.assertThat(decodedPayment.getDestinationAmount(),
        CoreMatchers.is(payment.getDestinationAmount()));
    MatcherAssert.assertThat(decodedPayment.getData(),
        CoreMatchers.is(payment.getData()));

    MatcherAssert.assertThat(decodedCondition, CoreMatchers.is(condition));

  }
}
