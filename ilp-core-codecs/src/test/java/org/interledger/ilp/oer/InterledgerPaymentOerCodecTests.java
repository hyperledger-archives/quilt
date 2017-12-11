package org.interledger.ilp.oer;

import org.interledger.InterledgerAddress;
import org.interledger.InterledgerPacket;
import org.interledger.codecs.InterledgerCodecContext;
import org.interledger.codecs.InterledgerCodecContextFactory;
import org.interledger.ilp.InterledgerPayment;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hyperledger.quilt.codecs.framework.Codec;
import org.hyperledger.quilt.codecs.framework.CodecContext;
import org.hyperledger.quilt.codecs.framework.CodecContextFactory;
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

/**
 * Unit tests to validate the {@link Codec} functionality for all {@link InterledgerPayment}
 * packets.
 */
@RunWith(Parameterized.class)
public class InterledgerPaymentOerCodecTests {

  // first data value (0) is default
  @Parameter
  public InterledgerPacket packet;

  /**
   * The data for this test...
   */
  @Parameters
  public static Collection<Object[]> data() {

    // This ByteArrayOutputStream contains a random amount of 32kb for testing purposes.
    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    for (int i = 0; i < 32768; i++) {
      byteArrayOutputStream.write(i);
    }

    return Arrays.asList(new Object[][]{{InterledgerPayment.builder()
        .destinationAccount(InterledgerAddress.builder().value("test3.foo").build())
        .destinationAmount(BigInteger.valueOf(100L)).data(new byte[]{}).build()},

        {InterledgerPayment.builder()
            .destinationAccount(InterledgerAddress.builder().value("test1.bar").build())
            .destinationAmount(BigInteger.valueOf(50L))
            .data(new byte[]{1, 2, 3, 4, 5, 6, 7, 8}).build()},

        {InterledgerPayment.builder()
            .destinationAccount(InterledgerAddress.builder().value("test1.bar").build())
            .destinationAmount(BigInteger.valueOf(50L))
            .data(byteArrayOutputStream.toByteArray()).build()},

    });
  }

  /**
   * The primary difference between this test and {@link #testInterledgerPaymentCodec()} is that
   * this context call specifies the type, whereas the test below determines the type from the
   * payload.
   */
  @Test
  public void testIndividualRead() throws IOException {
    final InterledgerCodecContext context = InterledgerCodecContextFactory.oer();
    final ByteArrayInputStream asn1OerPaymentBytes = constructAsn1OerPaymentBytes();

    final InterledgerPayment payment = context.read(InterledgerPayment.class, asn1OerPaymentBytes);
    MatcherAssert.assertThat(payment, CoreMatchers.is(packet));
  }

  /**
   * The primary difference between this test and {@link #testIndividualRead()} is that this context
   * determines the ipr type from the payload, whereas the test above specifies the type in the
   * method call.
   */
  @Test
  public void testInterledgerPaymentCodec() throws Exception {
    final InterledgerCodecContext context = InterledgerCodecContextFactory.oer();
    final ByteArrayInputStream asn1OerPaymentBytes = constructAsn1OerPaymentBytes();

    final InterledgerPacket decodedPacket = context.read(asn1OerPaymentBytes);
    MatcherAssert.assertThat(decodedPacket.getClass().getName(),
        CoreMatchers.is(packet.getClass().getName()));
    MatcherAssert.assertThat(decodedPacket, CoreMatchers.is(packet));
  }

  private ByteArrayInputStream constructAsn1OerPaymentBytes() throws IOException {
    final CodecContext context = CodecContextFactory.oer();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    context.write(packet, outputStream);

    return new ByteArrayInputStream(outputStream.toByteArray());
  }

}
