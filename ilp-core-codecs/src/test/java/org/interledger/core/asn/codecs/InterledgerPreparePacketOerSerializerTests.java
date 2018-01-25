package org.interledger.core.asn.codecs;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.asn.framework.InterledgerCodecContextFactory;
import org.interledger.cryptoconditions.PreimageSha256Condition;
import org.interledger.encoding.asn.framework.CodecContext;

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
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;

/**
 * Unit tests to validate the functionality for all
 * {@link InterledgerPreparePacket}
 * packets.
 */
@RunWith(Parameterized.class)
public class InterledgerPreparePacketOerSerializerTests {

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

    final byte[] condition = new byte[32];

    return Arrays.asList(new Object[][] {

        {InterledgerPreparePacket.builder()
            .destination(InterledgerAddress.of("test3.foo"))
            .amount(BigInteger.valueOf(100L))
            .executionCondition(new PreimageSha256Condition(32, condition))
            .expiresAt(Instant.now())
            .data(new byte[] {}).build()},

        {InterledgerPreparePacket.builder()
            .destination(InterledgerAddress.builder().value("test1.bar").build())
            .amount(BigInteger.valueOf(50L))
            .executionCondition(new PreimageSha256Condition(32, condition))
            .expiresAt(Instant.now())
            .data(new byte[] {1, 2, 3, 4, 5, 6, 7, 8}).build()},

        {InterledgerPreparePacket.builder()
            .destination(InterledgerAddress.builder().value("test1.bar").build())
            .amount(BigInteger.valueOf(50L))
            .executionCondition(new PreimageSha256Condition(32, condition))
            .expiresAt(Instant.now())
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
    final CodecContext context = InterledgerCodecContextFactory.oer();
    final ByteArrayInputStream asn1OerPaymentBytes = constructAsn1OerPaymentBytes();

    final InterledgerPreparePacket payment = context.read(InterledgerPreparePacket.class,
        asn1OerPaymentBytes);
    MatcherAssert.assertThat(payment, CoreMatchers.is(packet));
  }

  /**
   * The primary difference between this test and {@link #testIndividualRead()} is that this context
   * determines the ipr type from the payload, whereas the test above specifies the type in the
   * method call.
   */
  @Test
  public void testInterledgerPaymentCodec() throws Exception {
    final CodecContext context = InterledgerCodecContextFactory.oer();
    final ByteArrayInputStream asn1OerPaymentBytes = constructAsn1OerPaymentBytes();

    final InterledgerPacket decodedPacket = context.read(InterledgerPacket.class,
        asn1OerPaymentBytes);
    MatcherAssert.assertThat(decodedPacket.getClass().getName(),
        CoreMatchers.is(packet.getClass().getName()));
    MatcherAssert.assertThat(decodedPacket, CoreMatchers.is(packet));
  }

  private ByteArrayInputStream constructAsn1OerPaymentBytes() throws IOException {
    final CodecContext context = InterledgerCodecContextFactory.oer();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    context.write(packet, outputStream);

    return new ByteArrayInputStream(outputStream.toByteArray());
  }

}
