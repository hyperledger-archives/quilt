package org.interledger.encoding.asn.serializers.oer;

import org.interledger.encoding.asn.codecs.AsnUint8Codec;
import org.interledger.encoding.asn.framework.AsnObjectSerializationContext;
import org.interledger.encoding.asn.framework.AsnObjectSerializer;
import org.interledger.encoding.asn.framework.CodecException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * An extension of {@link AsnObjectSerializer} for reading and writing an ASN.1 OER 8-Bit
 * integer.
 *
 * <p>This serializer is used instead of the generic Octet String serializer for performance
 * reasons.
 */
public class AsnUint8OerSerializer implements AsnObjectSerializer<AsnUint8Codec> {

  @Override
  public void read(final AsnObjectSerializationContext context, final AsnUint8Codec instance,
                   final InputStream
      inputStream)
      throws IOException {
    Objects.requireNonNull(context);
    Objects.requireNonNull(instance);
    Objects.requireNonNull(inputStream);

    int value = inputStream.read();
    if (value == -1) {
      throw new CodecException("Unexpected end of stream.");
    }

    instance.encode(value);

  }

  @Override
  public void write(AsnObjectSerializationContext context, AsnUint8Codec instance,
                    OutputStream outputStream)
      throws IOException {

    if (instance.decode() > 255) {
      throw new IllegalArgumentException("UInt8 values may only contain up to 8 bits!");
    }

    outputStream.write(instance.decode());
  }

}
