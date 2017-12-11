package org.hyperledger.quilt.codecs.oer;

import org.hyperledger.quilt.codecs.framework.Codec;
import org.hyperledger.quilt.codecs.framework.CodecContext;
import org.hyperledger.quilt.codecs.framework.CodecException;
import org.hyperledger.quilt.codecs.oer.OerLengthPrefixCodec.OerLengthPrefix;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * <p>An extension of {@link Codec} for reading and writing an ASN.1 OER Length prefix octets.</p>
 * <p>A length prefix specifies the length of a subsequent encoded object in terms of number of
 * octets required to encoded that object.</p> <p>The following rules apply:</p> <p>If the number of
 * octets required to encode an object is less than 128, then that length is encoded in the
 * lowest-order 7 bit positions of the first and only octet. The highest-order bit of the octet is
 * set to zero.</p> <p>Conversely, the number of octets required to encode an object is greater than
 * 127, then then that length is encoded into 2 or more octets as follows: The first octet will have
 * its highest-order bit set to 1, with the remaining 7 octets representing the number of subsequent
 * octets required to encode a number representing the actual length of the encoded object.
 * Depending on the value of that first length number (called 'N' for now), the next N octets will
 * encode a number representing the number of octets required to encode the actual object.</p>
 * <p>All encodings are in big-endian order.</p>
 */
public class OerLengthPrefixCodec implements Codec<OerLengthPrefix> {

  @Override
  public OerLengthPrefix read(final CodecContext context, final InputStream inputStream)
      throws IOException {
    Objects.requireNonNull(context);
    Objects.requireNonNull(inputStream);

    // The number of encoded octets that the encoded payload will be stored in.
    final int numEncodedOctets;

    int initialLengthPrefixOctet = inputStream.read();
    if (initialLengthPrefixOctet >= 0 && initialLengthPrefixOctet < 128) {
      numEncodedOctets = initialLengthPrefixOctet;
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
      
      numEncodedOctets = toInt(ba);
    }

    return new OerLengthPrefix(numEncodedOctets);
  }

  @Override
  public void write(final CodecContext context, final OerLengthPrefix oerLengthPrefix,
      final OutputStream outputStream) throws IOException {

    Objects.requireNonNull(context);
    Objects.requireNonNull(oerLengthPrefix);
    Objects.requireNonNull(outputStream);

    final int length = oerLengthPrefix.getLength();

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

  /**
   * Helper method to convert a byte array of varying length (assuming not larger than 4 bytes) into
   * an int. This is necessary because most traditional library assume a 4-byte array when
   * converting to an Integer.
   *
   * @param bytes An array of up to 4 bytes representing an integer
   *
   * @return the int representation of the given bytes
   */
  protected int toInt(final byte[] bytes) {

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


  /**
   * An typing mechanism for registering codecs for length prefixes (as opposed to normal integer
   * types).
   */
  public static class OerLengthPrefix {

    private final int length;

    public OerLengthPrefix(final int length) {
      this.length = length;
    }

    public int getLength() {
      return length;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }

      OerLengthPrefix that = (OerLengthPrefix) obj;

      return length == that.length;
    }

    @Override
    public int hashCode() {
      return length;
    }

    @Override
    public String toString() {
      return "LengthPrefix{"
          + "length=" + length
          + '}';
    }
  }

}
