package org.interledger.codecs.oer.psk;

import static org.hamcrest.MatcherAssert.assertThat;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.interledger.codecs.CodecContext;
import org.interledger.codecs.CodecContextFactory;
import org.interledger.codecs.CodecException;
import org.interledger.codecs.psk.PskMessageBinaryCodec;
import org.interledger.ilp.InterledgerPayment;
import org.interledger.psk.PskEncryptionType;
import org.interledger.psk.PskMessage;
import org.interledger.psk.PskMessage.Header;
import org.junit.Test;


/**
 * Unit tests to validate {@link PskMessage} headers encoding/decoding with a
 * particular focus on ensuring serialization failure in the case of the presence of line feed or
 * carriage return characters ('\n' and '\r', respectively), as described in the instructions found
 * in Data Format section of IL-RFC-16.  The presence of line feed characters in headers can lead to
 * vulnerabilities from header injection attacks
 * {@link "https://en.wikipedia.org/wiki/HTTP_header_injection"}.  It is important that the
 * {@link PskMessageBinaryCodec} ensure both that a {@link PskMessage} with an invalid header can
 * neither be encoded nor decoded.
 *
 * @see "https://github.com/interledger/rfcs/blob/master/0016-pre-shared-key/0016-pre-shared-key.md#data-format"
 */
public class PskHeaderValidationTests {

  private static final char CR = '\r';
  private static final char LF = '\n';

  /**
   * Public headers
   */
  private static final PskMessage.Header goodPublicHeader =
      new Header("question", "What is the answer?");
  private static final PskMessage.Header badPublicHeader1 =
      new Header("question", "What's the \nsolution?");
  private static final PskMessage.Header badPublicHeader2 =
      new Header("question", "What's the \rsolution?");

  /**
   * Private headers
   */
  private static final PskMessage.Header goodPrivateHeader =
      new Header("answer", "Choice, the problem is choice.");
  private static final PskMessage.Header badPrivateHeader1 =
      new Header("answer1", "But we control these\n machines; they don't control us!");
  private static final PskMessage.Header badPrivateHeader2 =
      new Header("answer", "But we control these\r machines; they don't control us!");


  /**
   * Application Data
   */
  private static final byte[] applicationData =
      "{\"oracle\":\"candy\", \"forseen\":true}".getBytes();

  /**
   * This test serves as a control.  Not only must the {@link PskMessageBinaryCodec} reject
   * a {@link PskMessage} with public or private headers containing line feed or carriage return
   * characters, but it must also accept a {@link PskMessage} with headers not containing these
   * potentially nefarious characters.
   */
  @Test
  public void happyPathRoundTripTest() {

    final PskMessage pskMessage = PskMessage.builder()
        .addPrivateHeader(goodPrivateHeader).addPublicHeader(goodPublicHeader)
        .data(applicationData).build();

    try {
      final byte[] pskMessageBytes = CodecContextFactory.interledger().write(pskMessage);
      assertThat("A codec exception is not throw when we attempt to encode with an invalid header",
          true );

      PskMessage decoded = CodecContextFactory.interledger().read( PskMessage.class,
              new ByteArrayInputStream(pskMessageBytes));

      assertThat("A codec exception is not throw when we attempt to encode with an invalid header",
          true );

    }
    catch(IOException e) {
      assertThat("An IO exception is not throw when we attempt to decode with valid headers",
          false );
    }
    catch(CodecException e) {
      assertThat("A codec exception is not throw when we attempt to encode with valid headers",
          false );
    }

  }

  /**
   * Test encoding the different combination of valid and invalid public and private headers.
   */
  @Test
  public void testEncodingWithInvalidHeaders() {

    /**
     * Validate that a {@link PskMessage} with public headers with line feed characters ('\n') may
     * not be encoded.
     */
    runEncodingTest(goodPrivateHeader, badPublicHeader1);

    /**
     * Validate that a {@link PskMessage} with public headers with carriage return characters ('\r')
     * may not be encoded.
     */
    runEncodingTest(goodPrivateHeader, badPublicHeader2);

    /**
     * Validate that a {@link PskMessage} with private headers with line feed characters ('\n') may
     * not be encoded.
     */
    runEncodingTest(badPrivateHeader1, goodPublicHeader);

    /**
     * Validate that a {@link PskMessage} with private headers with carriage return characters
     * ('\r') may not be encoded.
     */
    runEncodingTest(badPrivateHeader2, goodPublicHeader);

  }

  /**
   * Test decoding the different combination of valid and invalid public and private headers.
   */
  @Test
  public void testDecodingWithInvalidHeaders() {

    /**
     * Validate that a {@link PskMessage} with public headers with line feed characters ('\n') may
     * not be decoded.
     */
    runDecodingTest(goodPrivateHeader, badPublicHeader1);

    /**
     * Validate that a {@link PskMessage} with public headers with carriage return characters ('\r')
     * may not be decoded.
     */
    runDecodingTest(goodPrivateHeader, badPublicHeader2);

    /**
     * Validate that a {@link PskMessage} with private headers with line feed characters ('\n') may
     * not be decoded.
     */
    runDecodingTest(badPrivateHeader1, goodPublicHeader);

    /**
     * Validate that a {@link PskMessage} with private headers with carriage return characters
     * ('\r') may not be decoded.
     */
    runDecodingTest(badPrivateHeader2, goodPublicHeader);

  }

  /**
   * A convenience method to test decoding with either an invalid private or public
   * {@link PskMessage.Header}.
   * @param privateHeader
   * @param publicHeader
   */
  public void runDecodingTest(PskMessage.Header privateHeader,
      PskMessage.Header publicHeader) {
    NefariousPskMessageBinaryCodec nefariousCodec = new NefariousPskMessageBinaryCodec();
    /* build the psk message with the supplied private and public headers */
    final PskMessage pskMessage = PskMessage.builder()
        .addPrivateHeader(privateHeader).addPublicHeader(publicHeader)
        .data(applicationData).build();
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

      /**
       * Encode the {@link PskMessage} with the {@link NefariousPskMessageBinaryCodec}.  This codec
       * bypasses the filtering present in the {@link PskMessageBinaryCodec}.
       */
      nefariousCodec
          .write(CodecContextFactory.interledger(), pskMessage, baos);

      /* attempt to decode the message with the standard implementation */
      PskMessage decodedPskMessage = CodecContextFactory.interledger().read(
          PskMessage.class,
          new ByteArrayInputStream( baos.toByteArray() ));

      /* if the decoding succeeds, then we have a problem */
      assertThat("An attempt to decode does not succeed with an invalid header", false );
    }
    catch(IOException e) {
      assertThat("An IO exception is not thrown when we attempt to encode with an invalid header",
          false );
    }
    catch(CodecException e) {
      /* we expect an exception to be thrown */
      assertThat("A CodecException is thrown when we attempt to encode with an invalid header",
          true );
    }

  }

  /**
   * A convenience method to test encoding with either an invalid private or public
   * {@link PskMessage.Header}.
   * @param privateHeader A private {@link PskMessage.Header} instance
   * @param publicHeader  A public {@link PskMessage.Header} instance
   */
  private void runEncodingTest(PskMessage.Header privateHeader, PskMessage.Header publicHeader) {
    /* build the psk message with the supplied private and public headers */
    final PskMessage pskMessage = PskMessage.builder()
        .addPrivateHeader(privateHeader).addPublicHeader(publicHeader)
        .data(applicationData).build();
    try {
      /* attempt to encode the psk message */
      final byte[] pskMessageBytes = CodecContextFactory.interledger().write(pskMessage);

      /* if the encoding succeeds, then we have a problem */
      assertThat("An attempt to encode does not succeed with an invalid header", false );
    }
    catch(CodecException e) {
      /* we expect an exception to be thrown */
      assertThat("A codec exception is thrown when we attempt to encode with an invalid header",
          true );
    }

  }

  /**
   * A sub-class of {@link PskMessageBinaryCodec} that bypasses header filtering.  This allows us
   * to create invalid {@link PskMessage} instances for testing purposes.
   */
  private static class NefariousPskMessageBinaryCodec extends PskMessageBinaryCodec {

    /**
     * Override the write method in the super-class.  We must do so, because we want to create a
     * {@link PskMessage} with invalid headers, and the
     * {@link PskMessageBinaryCodec#writeHeader(Header, Writer)} method is private.
     *
     * @param context The current {@link CodecContext}
     * @param instance The instance of {@link PskMessage} to write
     * @param outputStream The target {@link OutputStream} to which to write.
     * @throws IOException If we encounter an invalid IO condition.
     */
    @Override
    public void write(final CodecContext context, final PskMessage instance,
        final OutputStream outputStream) throws IOException {
      Objects.requireNonNull(context);
      Objects.requireNonNull(instance);
      Objects.requireNonNull(outputStream);

      OutputStreamWriter out = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
      BufferedWriter writer = new BufferedWriter(out);

      // Write Status
      writer.write(PskMessage.STATUS_LINE);
      writer.write(LF);

      // Write Public Headers
      for (PskMessage.Header header : instance.getPublicHeaders()) {
        writeHeader(header, writer);
      }
      writer.write(LF);

      if (instance.getEncryptionHeader()
          .getEncryptionType() == PskEncryptionType.NONE) {

        // Write Private Headers
        for (PskMessage.Header header : instance.getPrivateHeaders()) {
          writeHeader(header, writer);
        }
        writer.write(LF);

      }

      // Write Data
      writer.flush();
      outputStream.write(instance.getData());

    }

    /**
     * Write a {@link PskMessage.Header} without validation
     * @param header An instance of {@link PskMessage.Header} to write to the writer.
     * @param writer An instance of {@link Writer} to which to write the {@link PskMessage.Header}
     * @throws IOException If an invalid IO condition is encountered.
     */
    private void writeHeader(PskMessage.Header header, Writer writer) throws IOException {
      if (header == null) {
        return;
      }

      writer.write(header.getName());
      writer.write(':');
      writer.write(header.getValue());
      writer.write(LF);
    }

  }

}
