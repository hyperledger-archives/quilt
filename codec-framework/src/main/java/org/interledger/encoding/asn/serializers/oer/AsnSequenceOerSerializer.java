package org.interledger.encoding.asn.serializers.oer;

import org.interledger.encoding.asn.codecs.AsnSequenceCodec;
import org.interledger.encoding.asn.framework.AsnObjectSerializationContext;
import org.interledger.encoding.asn.framework.AsnObjectSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An extension of {@link AsnObjectSerializer} for reading and writing an ASN.1 OER
 * object that is represented by an SEQUENCE.
 */
public class AsnSequenceOerSerializer implements AsnObjectSerializer<AsnSequenceCodec> {

  @Override
  public void read(AsnObjectSerializationContext context, AsnSequenceCodec instance,
                   InputStream inputStream)
      throws IOException {
    for (int i = 0; i < instance.size(); i++) {
      context.read(instance.getCodecAt(i), inputStream);
    }
  }

  @Override
  public void write(AsnObjectSerializationContext context, AsnSequenceCodec instance, OutputStream
      outputStream) throws IOException {
    for (int i = 0; i < instance.size(); i++) {
      context.write(instance.getCodecAt(i), outputStream);
    }
  }
}
