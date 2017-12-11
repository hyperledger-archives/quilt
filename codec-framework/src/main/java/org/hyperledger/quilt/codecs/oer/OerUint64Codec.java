package org.hyperledger.quilt.codecs.oer;

import org.hyperledger.quilt.codecs.framework.Codec;
import org.hyperledger.quilt.codecs.framework.CodecContext;
import org.hyperledger.quilt.codecs.oer.OerUint64Codec.OerUint64;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Objects;

/**
 * <p>An extension of {@link Codec} for reading and writing an ASN.1 OER 64-Bit unsigned integer
 * type as defined by the Interledger ASN.1 definitions.</p> <p>All Interledger ASN.1 integer types
 * are encoded as fixed-size, non-extensible numbers. Thus, for a UInt64 type, the integer value is
 * encoded as an unsigned binary integer in 8 octets, and supports values in the range
 * (0..18446744073709551615). </p>
 */
public class OerUint64Codec implements Codec<OerUint64> {

  /**
   * ASN.1 64BitUInt: If the lower bound of the value range constraint is not less than 0 and the
   * upper bound is not greater than 18446744073709551615 and the constraint is not extensible, the
   * integer value is encoded as an unsigned binary integer in eight octets.
   *
   * @param context     An instance of {@link CodecContext}.
   * @param inputStream An instance of @link InputStream}.
   *
   * @throws IOException              If there is a problem writing to the {@code stream}.
   * @throws IllegalArgumentException If the input has a value greater than 18446744073709551615.
   */
  @Override
  public OerUint64 read(final CodecContext context, final InputStream inputStream)
      throws IOException {
    Objects.requireNonNull(context);
    Objects.requireNonNull(inputStream);

    byte[] value = new byte[8];
    int read = inputStream.read(value);

    if (read != 8) {
      throw new IOException("unexpected end of stream. expected 8 bytes, read " + read);
    }

    return new OerUint64(new BigInteger(1, value));
  }

  /**
   * ASN.1 64BitUInt: If the lower bound of the value range constraint is not less than 0 and the
   * upper bound is not greater than 18446744073709551615 and the constraint is not extensible, the
   * integer value is encoded as an unsigned binary integer in eight octets.
   *
   * @param context      An instance of {@link CodecContext}.
   * @param instance     An instance of {@link OerUint64}.
   * @param outputStream An instance of {@link OutputStream}.
   *
   * @throws IOException              If there is a problem writing to the {@code stream}.
   * @throws IllegalArgumentException If the input has a value greater than 18446744073709551615.
   */
  @Override
  public void write(final CodecContext context, final OerUint64 instance,
      final OutputStream outputStream) throws IOException, IllegalArgumentException {

    Objects.requireNonNull(context);
    Objects.requireNonNull(instance);
    Objects.requireNonNull(outputStream);

    byte[] value = instance.getValue()
        .toByteArray();
    
    /* BigInteger's toByteArray writes data in two's complement, so positive values requiring 64
     * bits will include a leading byte set to 0 which we don't want. */
    if (value.length > 8) {
      outputStream.write(value, value.length - 8, 8);
      return;
    }
    
    /* BigInteger.toByteArray will return the smallest byte array possible. We are committed
     * to a fixed number of bytes, so we might need to pad the value out. */
    for (int i = 0; i < 8 - value.length; i++) {
      outputStream.write(0);
    }
    outputStream.write(value);
  }

  /**
   * Merely a typing mechanism for registering multiple codecs that operate on the same type.
   */
  public static class OerUint64 {

    private final BigInteger value;

    /**
     * Constructs an OerUint64 instance.
     *
     * @param value The value to read or write as an OER 64-bit int value.
     **/
    public OerUint64(final BigInteger value) {
      this.value = value;
    }

    public BigInteger getValue() {
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

      OerUint64 oerUint64 = (OerUint64) obj;

      return value.equals(oerUint64.value);
    }

    @Override
    public int hashCode() {
      return value.hashCode();
    }

    @Override
    public String toString() {
      return "OerUint64{"
          + "value=" + value
          + '}';
    }
  }
}
