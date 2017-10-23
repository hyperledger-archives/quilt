package org.interledger.codecs.oer;

import org.interledger.codecs.Codec;
import org.interledger.codecs.CodecContext;
import org.interledger.codecs.oer.OerUint32Codec.OerUint32;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * <p>An extension of {@link Codec} for reading and writing an ASN.1 OER 32-Bit integer type as
 * defined by the Interledger ASN.1 definitions.</p>
 * <p>All Interledger ASN.1 integer types are encoded as fixed-size, non-extensible numbers.  Thus,
 * for a UInt32 type, the integer value is encoded as an unsigned binary integer in 4 octets, and
 * supports values in the range (0..4294967295).</p>
 */
public class OerUint32Codec implements Codec<OerUint32> {

  /**
   * ASN.1 32BitUInt: If the lower bound of the value range constraint is not less than 0 and the
   * upper bound is not greater than 4294967295 and the constraint is not extensible,
   * the integer value is encoded as an unsigned binary integer in four octets.
   *
   * @param context     An instance of {@link CodecContext}.
   * @param inputStream An instance of @link InputStream}.
   *
   * @throws IOException              If there is a problem writing to the {@code stream}.
   * @throws IllegalArgumentException If the input has a value greater than 4294967295.
   */
  @Override
  public OerUint32 read(final CodecContext context, final InputStream inputStream)
      throws IOException {
    Objects.requireNonNull(context);
    Objects.requireNonNull(inputStream);

    long value = 0;
    for (int i = 0; i <= 3; i++) {
      value <<= Byte.SIZE;
      value |= (inputStream.read() & 0xFF);
    }
    return new OerUint32(value);
  }

  /**
   * ASN.1 32BitUInt: If the lower bound of the value range constraint is not less than 0 and the
   * upper bound is not greater than 4294967295 and the constraint is not extensible, the integer
   * value is encoded as an unsigned binary integer in four octets.
   *
   * @param context      An instance of {@link CodecContext}.
   * @param instance     An instance of {@link OerUint32}.
   * @param outputStream An instance of {@link OutputStream}.
   *
   * @throws IOException              If there is a problem writing to the {@code stream}.
   * @throws IllegalArgumentException If the input is out of range.
   */
  @Override
  public void write(
      final CodecContext context, final OerUint32 instance, final OutputStream outputStream
  ) throws IOException, IllegalArgumentException {

    Objects.requireNonNull(context);
    Objects.requireNonNull(instance);
    Objects.requireNonNull(outputStream);

    if (instance.getValue() > 4294967295L || instance.getValue() < 0) {
      throw new IllegalArgumentException(
          "Interledger Uint32 only supports values from 0 to 4294967295, value "
              + instance.getValue() + " is out of range.");
    }

    long value = instance.getValue();
    for (int i = 3; i >= 0; i--) {
      byte octet = ((byte) ((value >> (Byte.SIZE * i)) & 0xFF));
      outputStream.write(octet);
    }
  }

  /**
   * Merely a typing mechanism for registering multiple codecs that operate on the same type.
   */
  public static class OerUint32 {

    private final long value;

    public OerUint32(final long value) {
      this.value = value;
    }

    public long getValue() {
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

      OerUint32 oerUint32 = (OerUint32) obj;

      return value == oerUint32.value;
    }

    @Override
    public int hashCode() {
      return (int) (value ^ (value >>> 32));
    }

    @Override
    public String toString() {
      return "OerUint32{"
          + "value=" + value
          + '}';
    }
  }
}
