package org.interledger.encoding.asn.serializers.oer;

import static java.lang.String.format;

import org.interledger.encoding.asn.codecs.AsnOctetStringBasedObjectCodec;
import org.interledger.encoding.asn.codecs.AsnOpenTypeCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;
import org.interledger.encoding.asn.framework.AsnObjectCodec;
import org.interledger.encoding.asn.framework.AsnObjectSerializationContext;
import org.interledger.encoding.asn.framework.AsnObjectSerializer;
import org.interledger.encoding.asn.framework.CodecException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * An extension of {@link AsnObjectSerializer} for reading and writing an ASN.1 OER
 * object that is represented by an octet string.
 */
public class AsnOpenTypeOerSerializer
    implements AsnObjectSerializer<AsnOpenTypeCodec> {

  @Override
  public void read(final AsnObjectSerializationContext context,
                   final AsnOpenTypeCodec instance,
                   final InputStream inputStream) throws IOException {
    Objects.requireNonNull(context);
    Objects.requireNonNull(instance);
    Objects.requireNonNull(inputStream);

    //Get length
    int length = OerLengthSerializer.readLength(inputStream);

    //Read in correct number of bytes
    byte[] buffer = new byte[length];
    int bytesRead = inputStream.read(buffer);

    if (bytesRead != length) {
      throw new IOException(format("Unexpected end of stream. Expected %s bytes but got %s.",
          length, bytesRead));
    }
    context.read(instance.getInnerCodec(), buffer);

  }

  @Override
  public void write(final AsnObjectSerializationContext context,
                    final AsnOpenTypeCodec instance,
                    final OutputStream outputStream) throws IOException {
    Objects.requireNonNull(context);
    Objects.requireNonNull(instance);
    Objects.requireNonNull(outputStream);

    //Serialize the inner Asn.1 object
    final byte[] bytes = context.write(instance.getInnerCodec());

    //Write a length prefix
    OerLengthSerializer.writeLength(bytes.length, outputStream);

    //Write the object
    if (bytes.length > 0) {
      outputStream.write(bytes);
    }
  }

}
