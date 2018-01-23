package org.interledger.codecs.oer;

import org.interledger.codecs.asn.AsnUint8;
import org.interledger.codecs.framework.AsnObjectSerializer;
import org.interledger.codecs.framework.CodecException;
import org.interledger.codecs.framework.SerializationContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * <p>An extension of {@link AsnObjectSerializer} for reading and writing an ASN.1 OER 8-Bit
 * integer.
 * <p>This serializer is used instead of the generic Octet String serializer for performance
 * reasons.</p>
 */
public class AsnUint8OerSerializer implements AsnObjectSerializer<AsnUint8> {

  @Override
  public void read(final SerializationContext context, final AsnUint8 instance, final InputStream
      inputStream)
      throws IOException {
    Objects.requireNonNull(context);
    Objects.requireNonNull(instance);
    Objects.requireNonNull(inputStream);

    int value = inputStream.read();
    if (value == -1) {
      throw new CodecException("Unexpected end of stream.");
    }

    instance.setValue(value);

  }

  @Override
  public void write(SerializationContext context, AsnUint8 instance, OutputStream outputStream)
      throws IOException {

    if (instance.getValue() > 255) {
      throw new IllegalArgumentException("UInt8 values may only contain up to 8 bits!");
    }

    outputStream.write(instance.getValue());
  }

}
