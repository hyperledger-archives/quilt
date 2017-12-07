package org.interledger.codecs.oer.psk;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.interledger.InterledgerAddress;
import org.interledger.InterledgerPacket;
import org.interledger.codecs.Codec;
import org.interledger.codecs.CodecContext;
import org.interledger.codecs.CodecContextFactory;
import org.interledger.ilp.InterledgerPayment;
import org.interledger.psk.PskEncryptionType;
import org.interledger.psk.PskMessage;
import org.interledger.psk.PskMessage.Header;

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
 * Unit tests to validate the {@link Codec} functionality for all {@link InterledgerPayment} packets
 * that include PSK payload data. This test was assembled using the pseudocode instructions found in
 * IL-RFC-16.
 *
 * @see "https://github.com/interledger/rfcs/blob/master/0016-pre-shared-key/0016-pre-shared-key.md#pseudocode"
 */
@RunWith(Parameterized.class)
public class InterledgerPaymentWithPskCodecTests {

  private static final PskMessage.Header publicHeader1 =
      new Header("question", "What is the answer?");
  private static final PskMessage.Header publicHeader2 =
      new Header("question", "What's the solution?");
  private static final PskMessage.Header privateHeader1 =
      new Header("answer", "Choice, the problem is choice.");
  private static final PskMessage.Header privateHeader2 =
      new Header("answer", "But we control these machines; they don't control us!");
  private static final byte[] applicationData =
      "{\"oracle\":\"candy\", \"forseen\":true}".getBytes();
  // first data value (0) is default
  @Parameter
  public InterledgerPayment interledgerPayment;
  @Parameter(1)
  public PskMessage pskMessage;

  /**
   * The data for this test...
   */
  @Parameters
  public static Collection<Object[]> data() throws IOException {

    /*
     * A sharedSecretKey is created in order to encrypt the private headers and the application
     * data. A nonce is used to generate the sharedSecretKey, so that the same private headers will
     * not produce the same ciphertext. Also note that the nonce's inclusion in the ILP payment
     * ensures that multiple payments using the same shared secret result in different hashes. Note
     * that the nonce is added automatically by the message builder unless a nonce header is
     * specifically added.
     */

    final PskMessage pskMessage = PskMessage.builder().addPrivateHeader(privateHeader1)
        .addPrivateHeader(privateHeader2).addPublicHeader(publicHeader1)
        .addPublicHeader(publicHeader2).data(applicationData).build();

    final byte[] pskMessageBytes = CodecContextFactory.interledger().write(pskMessage);

    return Arrays.asList(new Object[][]{
        {InterledgerPayment.builder()
            .destinationAccount(InterledgerAddress.of("test1.foo"))
            .destinationAmount(BigInteger.valueOf(100L)).data(pskMessageBytes).build(), pskMessage},

        {InterledgerPayment.builder()
            .destinationAccount(InterledgerAddress.of("test2.bar"))
            .destinationAmount(BigInteger.valueOf(1L)).data(pskMessageBytes).build(), pskMessage},

        {InterledgerPayment.builder()
            .destinationAccount(InterledgerAddress.of("test3.bar"))
            .destinationAmount(BigInteger.ZERO).data(pskMessageBytes).build(), pskMessage},

    });
  }

  @Test
  public void testWriteRead() throws Exception {
    final CodecContext context = CodecContextFactory.interledger();

    // Write the payment to ASN.1...
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    context.write(interledgerPayment, outputStream);

    // Read the bytes using Codecs...
    final ByteArrayInputStream asn1OerPaymentBytes =
        new ByteArrayInputStream(outputStream.toByteArray());

    final InterledgerPacket decodedPacket = context.read(asn1OerPaymentBytes);
    assertThat(decodedPacket.getClass().getName(), is(interledgerPayment.getClass().getName()));
    assertThat(decodedPacket, is(interledgerPayment));

    // Validate the PSK Info....
    new InterledgerPacket.VoidHandler.AbstractVoidHandler.HelperHandler() {
      @Override
      protected void handle(final InterledgerPayment decodedPayment) {
        assertThat(decodedPayment.getDestinationAccount(),
            is(interledgerPayment.getDestinationAccount()));
        assertThat(decodedPayment.getDestinationAmount(),
            is(interledgerPayment.getDestinationAmount()));

        final PskMessage decodedPskMessage =
            context.read(PskMessage.class, decodedPayment.getData());

        assertThat(decodedPskMessage.getData(), is(pskMessage.getData()));
        pskMessage.getPrivateHeaders()
            .forEach(header -> assertTrue(String.format("Header was not found: %s", header),
                decodedPskMessage.getPrivateHeaders(header.getName()).contains(header)));
        pskMessage.getPublicHeaders()
            .forEach(header -> assertTrue(String.format("Header was not found: %s", header),
                decodedPskMessage.getPublicHeaders(header.getName()).contains(header)));

        assertEquals(decodedPskMessage.getEncryptionHeader().getEncryptionType(),
            PskEncryptionType.NONE);

      }
    }.execute(decodedPacket);
  }

}
