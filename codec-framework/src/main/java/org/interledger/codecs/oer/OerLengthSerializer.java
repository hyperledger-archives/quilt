package org.interledger.codecs.oer;

import org.interledger.codecs.asn.AsnObject;
import org.interledger.codecs.framework.AsnObjectSerializer;
import org.interledger.codecs.framework.CodecException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

public class OerLengthSerializer  {

  public static int readLength(final InputStream inputStream) throws IOException {

    Objects.requireNonNull(inputStream);


    // The number of encoded octets that the encoded payload will be stored in.
    final int length;

    int initialLengthPrefixOctet = inputStream.read();
    if (initialLengthPrefixOctet >= 0 && initialLengthPrefixOctet < 128) {
      length = initialLengthPrefixOctet;
    } else {
      // Truncate the MSB and use the rest as a number...
      final int lengthOfLength = initialLengthPrefixOctet & 0x7f;

      // Convert the bytes into an integer...
      byte[] ba = new byte[lengthOfLength];
      int read = inputStream.read(ba, 0, lengthOfLength);

      if (read != lengthOfLength) {
        throw new IOException(
            "error reading " + lengthOfLength + " bytes from stream, only read " + read);
      }
      length = toInt(ba);
    }

    return length;
  }

  /**
   * Helper method to convert a byte array of varying length (assuming not larger than 4 bytes) into
   * an int. This is necessary because most traditional library assume a 4-byte array when
   * converting to an Integer.
   *
   * @param bytes An array of up to 4 bytes representing an integer
   * @return the int representation of the given bytes
   */
  private static int toInt(final byte[] bytes) {

    switch (bytes.length) {
      case 0:
        return 0;
      case 1: {
        return (bytes[0]) & 0x000000ff;
      }
      case 2: {
        return (bytes[0] << 8) & 0x0000ff00
            | (bytes[1]) & 0x000000ff;
      }
      case 3: {
        return (bytes[0] << 16) & 0x00ff0000
            | (bytes[1] << 8) & 0x0000ff00
            | (bytes[2]) & 0x000000ff;
      }
      case 4: {
        return (bytes[0] << 24) & 0xff000000
            | (bytes[1] << 16) & 0x00ff0000
            | (bytes[2] << 8) & 0x0000ff00
            | (bytes[3]) & 0x000000ff;
      }
      default: {
        throw new CodecException("This method only supports arrays up to length 4!");
      }
    }
  }

  public static void writeLength(final int length, final OutputStream outputStream) throws IOException {

    Objects.requireNonNull(outputStream);

    if (length >= 0 && length < 128) {

      // Write a single byte that contains the length (it will start with a 0, and not exceed 127 in
      // Base10.
      outputStream.write(length);
    } else {
      // Write the number of octets required to encode the length, and move the cursor to the
      // correct position.
      if (length <= 127) {
        // Write the first byte
        outputStream.write(length);
        // return 1;
      } else if (length <= 255) {
        // Write the first byte
        outputStream.write(128 + 1);
        outputStream.write(length);
        // return 2;
      } else if (length <= 65535) {
        outputStream.write(128 + 2);
        // Write the first byte, then the second byte.
        outputStream.write((length >> 8));
        outputStream.write(length);
        // return 3;
      } else if (length <= 16777215) {
        outputStream.write(128 + 3);
        // Write three bytes
        outputStream.write((length >> 16));
        outputStream.write((length >> 8));
        outputStream.write(length);
        // return 4;
      } else {
        outputStream.write(128 + 4);
        // Write four bytes,
        outputStream.write((length >> 24));
        outputStream.write((length >> 16));
        outputStream.write((length >> 8));
        outputStream.write(length);
      }
    }
  }
}