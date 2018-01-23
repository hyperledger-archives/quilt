package org.interledger.codecs.oer;

import static java.lang.String.format;

import org.interledger.codecs.asn.AsnOctetStringBasedObject;
import org.interledger.codecs.asn.AsnSizeConstraint;
import org.interledger.codecs.framework.AsnObjectSerializer;
import org.interledger.codecs.framework.CodecException;
import org.interledger.codecs.framework.SerializationContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * <p>An extension of {@link AsnObjectSerializer} for reading and writing an ASN.1 OER
 * object that is represented by an octet string.</p>
 */
public class AsnOctetStringOerSerializer implements AsnObjectSerializer<AsnOctetStringBasedObject> {

  @Override
  public void read(final SerializationContext context, final AsnOctetStringBasedObject instance,
                   final InputStream inputStream) throws IOException {
    Objects.requireNonNull(context);
    Objects.requireNonNull(instance);
    Objects.requireNonNull(inputStream);

    int length;
    final AsnSizeConstraint sizeConstraint = instance.getSizeConstraint();
    if (sizeConstraint.isFixedSize()) {
      length = sizeConstraint.getMax();
    } else {
      // Read the length of the encoded OctetString...
      length = OerLengthSerializer.readLength(inputStream);
    }

    final byte[] returnable = new byte[length];

    if (length == 0) {
      instance.setBytes(returnable);
    } else {

      int bytesRead = inputStream.read(returnable);
      if (bytesRead < length) {
        throw new CodecException(
            format("Unexpected end of stream. Expected %s bytes but only read %s.",
                length, bytesRead));
      }
      instance.setBytes(returnable);
    }

  }

  @Override
  public void write(final SerializationContext context, final AsnOctetStringBasedObject instance,
                    final OutputStream outputStream) throws IOException {
    Objects.requireNonNull(context);
    Objects.requireNonNull(instance);
    Objects.requireNonNull(outputStream);

    final byte[] bytes = instance.getBytes();
    final AsnSizeConstraint sizeConstraint = instance.getSizeConstraint();

    if (!sizeConstraint.isFixedSize()) {
      // Write the length of the encoded OctetString...
      OerLengthSerializer.writeLength(bytes.length, outputStream);
    }

    // Write the OctetString bytes to the buffer.
    if (bytes.length > 0) {
      outputStream.write(bytes);
    }
  }

}
