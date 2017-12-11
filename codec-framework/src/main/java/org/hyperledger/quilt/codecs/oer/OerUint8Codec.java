package org.hyperledger.quilt.codecs.oer;

import org.hyperledger.quilt.codecs.framework.Codec;
import org.hyperledger.quilt.codecs.framework.CodecContext;
import org.hyperledger.quilt.codecs.oer.OerUint8Codec.OerUint8;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * <p>An extension of {@link Codec} for reading and writing an ASN.1 OER 8-Bit integer type as
 * defined by the Interledger ASN.1 definitions.</p> <p>All Interledger ASN.1 integer types are
 * encoded as fixed-size, non-extensible numbers. Thus, for a UInt8 type, the integer value is
 * encoded as an unsigned binary integer in one octet.</p>
 */
public class OerUint8Codec implements Codec<OerUint8> {

  @Override
  public OerUint8 read(final CodecContext context, final InputStream inputStream)
      throws IOException {
    Objects.requireNonNull(context);
    Objects.requireNonNull(inputStream);

    return new OerUint8(inputStream.read());
  }

  @Override
  public void write(CodecContext context, OerUint8 instance, OutputStream outputStream)
      throws IOException {

    if (instance.getValue() > 255) {
      throw new IllegalArgumentException("Interledger UInt8 values may only contain up to 8 bits!");
    }

    outputStream.write(instance.getValue());
  }

  /**
   * Merely a typing mechanism for registering multiple codecs that operate on the same type.
   */
  public static class OerUint8 {

    private final int value;

    public OerUint8(final int value) {
      this.value = value;
    }

    public int getValue() {
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

      OerUint8 that = (OerUint8) obj;

      return value == that.value;
    }

    @Override
    public int hashCode() {
      return value;
    }

    @Override
    public String toString() {
      return "OerUint8{"
          + "value=" + value
          + '}';
    }
  }
}
