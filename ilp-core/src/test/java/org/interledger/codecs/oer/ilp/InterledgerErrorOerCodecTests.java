package org.interledger.codecs.oer.ilp;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.interledger.InterledgerAddress;
import org.interledger.InterledgerPacket;
import org.interledger.codecs.Codec;
import org.interledger.codecs.CodecContext;
import org.interledger.codecs.CodecContextFactory;
import org.interledger.ilp.InterledgerProtocolError;
import org.interledger.ilp.InterledgerProtocolError.ErrorCode;

import com.google.common.collect.ImmutableList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.time.Instant;

import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Unit tests to validate the {@link Codec} functionality for all {@link InterledgerProtocolError}
 * packets.
 */
@RunWith(Parameterized.class)
public class InterledgerErrorOerCodecTests {

  private static final InterledgerAddress FOO = InterledgerAddress.of("test1.foo");
  private static final InterledgerAddress BAR = InterledgerAddress.of("test1.bar");
  private static final InterledgerAddress BAZ = InterledgerAddress.of("test1.baz");

  private static final Instant NOW1 = Instant.now();
  private static final Instant NOW2 = Instant.now();
  private static final Instant NOW3 = Instant.now();

  // first data value (0) is default
  @Parameter
  public InterledgerPacket packet;

  /**
   * The data for this test...
   */
  @Parameters
  public static Collection<Object[]> data() throws IOException {

    final Random r = new Random();

    // This ByteArrayOutputStream contains a random amount of 32kb for testing purposes.
    final ByteArrayOutputStream byteArrayOutputStream1 = new ByteArrayOutputStream();
    IntStream.range(1, 32769)
        .map(r::nextInt)
        .forEach(byteArrayOutputStream1::write);

    // This ByteArrayOutputStream contains a random amount of 32kb for testing purposes.
    final ByteArrayOutputStream byteArrayOutputStream2 = new ByteArrayOutputStream();
    IntStream.range(1, 32769)
        .map(r::nextInt)
        .forEach(byteArrayOutputStream2::write);

    // This ByteArrayOutputStream contains a random amount of 32kb for testing purposes.
    final ByteArrayOutputStream byteArrayOutputStream3 = new ByteArrayOutputStream();
    IntStream.range(1, 32769)
        .map(r::nextInt)
        .forEach(byteArrayOutputStream3::write);

    return Arrays.asList(new Object[][]{
        {new InterledgerProtocolError.Builder()
            .errorCode(ErrorCode.T00_INTERNAL_ERROR)
            .triggeredByAddress(FOO)
            .forwardedByAddresses(ImmutableList.of(BAR, BAZ))
            .triggeredAt(NOW1)
            .data(byteArrayOutputStream1.toByteArray())
            .build()
        },

        {
            new InterledgerProtocolError.Builder()
                .errorCode(ErrorCode.T01_LEDGER_UNREACHABLE)
                .triggeredByAddress(BAR)
                .forwardedByAddresses(ImmutableList.of(FOO, BAZ))
                .triggeredAt(NOW2)
                .data(byteArrayOutputStream2.toByteArray())
                .build()
        },

        {
            new InterledgerProtocolError.Builder()
                .errorCode(ErrorCode.T02_LEDGER_BUSY)
                .triggeredByAddress(BAZ)
                .forwardedByAddresses(ImmutableList.of(FOO, BAR))
                .triggeredAt(NOW3)
                .data(byteArrayOutputStream3.toByteArray())
                .build()
        },

    });
  }

  /**
   * The primary difference between this test and {@link #testInterledgerErrorCodec()} is that this
   * context call specifies the type, whereas the test below determines the type from the payload.
   */
  @Test
  public void testIndividualRead() throws IOException {
    final CodecContext context = CodecContextFactory.interledger();
    final ByteArrayInputStream asn1OerErrorBytes = constructInterledgerProtocolErrorAsn1OerBytes();

    final InterledgerProtocolError error = context
        .read(InterledgerProtocolError.class, asn1OerErrorBytes);
    assertThat(error, is(packet));
  }

  /**
   * The primary difference between this test and {@link #testIndividualRead()} is that this context
   * determines the ipr type from the payload, whereas the test above specifies the type in the
   * method call.
   */
  @Test
  public void testInterledgerErrorCodec() throws Exception {
    final CodecContext context = CodecContextFactory.interledger();
    final ByteArrayInputStream asn1OerErrorBytes = constructInterledgerProtocolErrorAsn1OerBytes();

    final InterledgerPacket decodedPacket = context.read(asn1OerErrorBytes);
    assertThat(decodedPacket.getClass().getName(), is(packet.getClass().getName()));
    assertThat(decodedPacket, is(packet));
  }

  private ByteArrayInputStream constructInterledgerProtocolErrorAsn1OerBytes() throws IOException {
    final CodecContext context = CodecContextFactory.interledger();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    context.write(packet, outputStream);

    return new ByteArrayInputStream(outputStream.toByteArray());
  }

}
