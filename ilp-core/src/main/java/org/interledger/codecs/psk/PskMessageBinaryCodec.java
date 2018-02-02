package org.interledger.codecs.psk;

import org.interledger.codecs.Codec;
import org.interledger.codecs.CodecContext;
import org.interledger.codecs.CodecException;
import org.interledger.codecs.PskMessageCodec;
import org.interledger.psk.PskEncryptionType;
import org.interledger.psk.PskMessage;
import org.interledger.psk.PskMessage.Header;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * An implementation of {@link Codec} that reads and writes instances of {@link PskMessage}.
 */
public class PskMessageBinaryCodec implements PskMessageCodec {

  private static final char CR = '\r';
  private static final char LF = '\n';

  /**
   * Reads data of the input stream until a line feed character ('\n') is found or the input
   * stream is exhausted. Returns the data interpreted as a *UTF-8* string. Note that lines ending
   * with '\n' or '\r\n' are treated the same, the trailing '\r' will be removed if present.
   *
   * <p>A single '\r' (not followed immediately by a '\n') is not treated as an end of line.
   *
   * @return A UTF-8 encoded string read from the input stream.
   */
  private static String readLine(InputStream in) throws IOException {

    boolean readCarriageReturn = false;

    try (ByteArrayOutputStream bos = new ByteArrayOutputStream(256)) {
      int byteValue;
      while ((byteValue = in.read()) != -1) {

        if (byteValue == CR) {
          // Found CR, only write it if next char is NOT a LF
          readCarriageReturn = true;
          continue;
        }

        if (byteValue == LF) {
          break;
        }

        if (readCarriageReturn) {
          // Not end of the line but we read a CR previously so write it before the current byte
          bos.write(CR);
          readCarriageReturn = false;
        }

        bos.write(byteValue);
      }

      return bos.toString(StandardCharsets.UTF_8.name());
    }
  }

  /**
   * Consumes the remaining data in the stream, returning it as a byte array.
   */
  private static byte[] readRemainingBytes(InputStream in) throws IOException {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream(256)) {
      /*
       * copy the remaining data of the input stream to the output stream in blocks of 256 bytes
       */
      byte[] buffer = new byte[256];
      int length;
      while ((length = in.read(buffer)) != -1) {
        out.write(buffer, 0, length);
      }
      return out.toByteArray();
    }
  }

  @Override
  public PskMessage read(final CodecContext context, final InputStream inputStream)
      throws IOException {
    return read(inputStream, ReadState.STATUS_LINE);
  }

  private PskMessage read(final InputStream inputStream,
      ReadState state) throws IOException {

    Objects.requireNonNull(inputStream);

    PskMessage.Builder builder = PskMessage.builder();
    ReadState currentState = state;

    while (true) {

      if (currentState == ReadState.DATA) {
        builder.data(readRemainingBytes(inputStream));
        break;
      }

      String line = readLine(inputStream);
      if (line == null) {
        throw new CodecException("Premature end of stream.");
      }

      switch (currentState) {
        case STATUS_LINE:

          if (!line.equalsIgnoreCase(PskMessage.STATUS_LINE)) {
            throw new CodecException("Expecting valid status line but got [" + line + "]");
          }
          currentState = ReadState.PUBLIC_HEADERS;
          break;

        case PUBLIC_HEADERS:

          if (line.length() == 0) {
            currentState = builder.usesEncryption() ? ReadState.DATA : ReadState.PRIVATE_HEADERS;
          } else {
            builder.addPublicHeader(parseHeader(line));
          }
          break;
        case PRIVATE_HEADERS:

          if (line.length() == 0) {
            currentState = ReadState.DATA;
          } else {
            builder.addPrivateHeader(parseHeader(line));
          }
          break;

        case DATA:
        default:
          // We'll never get here
          break;
      }
    }

    return builder.build();

  }

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
   * Parse the private data portion of a PSK Message.
   *
   * <p>This uses the standard read method but starts in the private headers state.
   *
   * @param data The private data of a PSK message (headers and data separated by an empty line).
   *
   * @return A new PSK Message with only private headers and data.
   */
  public PskMessage parsePrivateData(byte[] data) {

    try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
      return read(bais, ReadState.PRIVATE_HEADERS);
    } catch (IOException e) {
      throw new CodecException("Error parsing private data.", e);
    }

  }

  private PskMessage.Header parseHeader(String line) {

    int split = line.indexOf(":");
    if (split == -1) {
      throw new CodecException("Invalid Public Header [" + line + "]. Expected ':' separator.");
    }

    final String name = line.substring(0, split)
        .trim();
    final String value = line.substring(split + 1)
        .trim();

    /**
     * Validate the {@link PskMessage} header does not contain line feed characters ('\n' and '\r'),
     * as described in the instructions found in Data Format section of IL-RFC-16.  The presence of
     * line feed characters in headers can lead to vulnerabilities from header injection attacks
     * {@link "https://en.wikipedia.org/wiki/HTTP_header_injection"}
     */
    if (value.contains("\n")) {
      throw new CodecException(
          String.format(
              "The value of the PskMessage header '%s' contains a line feed character. " +
                  "This is not allowed.", name));
    }
    if (value.contains("\r")) {
      throw new CodecException(
          String.format(
              "The value of the PskMessage header '%s' contains a carriage return character. " +
                  " This is not allowed.", name));
    }



    return new Header(name, value);
  }

  private void writeHeader(PskMessage.Header header, Writer writer) throws IOException {
    if (header == null) {
      return;
    }

    /**
     * Validate the {@link PskMessage} header does not contain line feed characters ('\n' and '\r'),
     * as described in the instructions found in Data Format section of IL-RFC-16.  The presence of
     * line feed characters in headers can lead to vulnerabilities from header injection attacks
     * {@link "https://en.wikipedia.org/wiki/HTTP_header_injection"}
     */
    if (header.getValue().contains("\n")) {
      throw new CodecException(
          String.format(
              "The value of the PskMessage header '%s' contains a line feed character. " +
                  "This is not allowed.", header.getName()));
    }
    if (header.getValue().contains("\r")) {
      throw new CodecException(
          String.format(
              "The value of the PskMessage header '%s' contains a carriage return character. " +
                  " This is not allowed.", header.getName()));
    }

    writer.write(header.getName());
    writer.write(':');
    writer.write(header.getValue());
    writer.write(LF);
  }

  /**
   * Write the private headers and data of the message to a stream and return as a byte array.
   *
   * @param message The decrypted PSK message
   *
   * @return private headers and data encoded to bytes
   */
  public byte[] writePrivateData(PskMessage message) {

    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      OutputStreamWriter out = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
      BufferedWriter writer = new BufferedWriter(out);

      // Write Private Headers
      for (PskMessage.Header header : message.getPrivateHeaders()) {
        writeHeader(header, writer);
      }
      writer.write(LF);

      // Write Data
      writer.flush();
      baos.write(message.getData());
      return baos.toByteArray();

    } catch (IOException e) {
      throw new CodecException("Error writing private data.", e);
    }

  }

  private enum ReadState {
    STATUS_LINE,
    PUBLIC_HEADERS,
    PRIVATE_HEADERS,
    DATA,
  }


}
