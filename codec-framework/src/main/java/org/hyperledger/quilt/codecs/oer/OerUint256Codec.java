package org.hyperledger.quilt.codecs.oer;

import static java.lang.String.format;

import org.hyperledger.quilt.codecs.framework.Codec;
import org.hyperledger.quilt.codecs.framework.CodecContext;
import org.hyperledger.quilt.codecs.framework.CodecException;
import org.hyperledger.quilt.codecs.oer.OerUint256Codec.OerUint256;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;

/**
 * <p> An extension of {@link Codec} for reading and writing an ASN.1 OER 256-Bit integer type as
 * defined by the Interledger ASN.1 definitions. </p> <p> All Interledger ASN.1 integer types are
 * encoded as fixed-size, non-extensible numbers. Thus, for a UInt256 type, the integer value is
 * encoded as an unsigned binary integer in 32 octets. </p>
 */
public class OerUint256Codec implements Codec<OerUint256> {

  /**
   * ASN.1 256BitUInt: Alias for a fixed length octet string of 32 octets.
   *
   * @param context     An instance of {@link CodecContext}.
   * @param inputStream An instance of @link InputStream}.
   *
   * @throws IOException              If there is a problem writing to the {@code stream}.
   * @throws IllegalArgumentException If the input has a value greater than 18446744073709551615.
   */
  @Override
  public OerUint256 read(final CodecContext context, final InputStream inputStream)
      throws IOException {
    Objects.requireNonNull(context);
    Objects.requireNonNull(inputStream);

    final byte[] returnable = new byte[32];
    int bytesRead = inputStream.read(returnable);

    if (bytesRead != 32) {
      throw new CodecException(
          format("Attempted to read a UInt256 and only got %s bytes.", bytesRead));
    }

    return new OerUint256(returnable);
  }

  /**
   * ASN.1 256BitUInt: Alias for a fixed length octet string of 32 octets.
   *
   * @param context      An instance of {@link CodecContext}.
   * @param instance     An instance of {@link OerUint256}.
   * @param outputStream An instance of {@link OutputStream}.
   *
   * @throws IOException              If there is a problem writing to the {@code stream}.
   * @throws IllegalArgumentException If the input has a value greater than 18446744073709551615.
   */
  @Override
  public void write(final CodecContext context, final OerUint256 instance,
      final OutputStream outputStream) throws IOException, IllegalArgumentException {

    Objects.requireNonNull(context);
    Objects.requireNonNull(instance);
    Objects.requireNonNull(outputStream);

    outputStream.write(instance.getValue());
  }

  /**
   * Merely a typing mechanism for registering multiple codecs that operate on the same type.
   */
  public static class OerUint256 {

    private final byte[] value;

    /**
     * Create a new OerUint256 from the given byte array.
     *
     * @param value a byte array of 32 bytes.
     */
    public OerUint256(final byte[] value) {
      if (value.length != 32) {
        throw new IllegalArgumentException("Value must be exactly 32 bytes.");
      }

      this.value = value;
    }

    public byte[] getValue() {
      return value;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }

      OerUint256 oerUint256 = (OerUint256) obj;

      return Arrays.equals(value, oerUint256.value);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + Arrays.hashCode(value);
      return result;
    }


    @Override
    public String toString() {
      return "OerUint256{"
          + "value=" + Arrays.toString(value)
          + '}';
    }
  }
}
