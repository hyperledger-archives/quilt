package org.interledger.encoding.asn.serializers.oer;

import org.interledger.encoding.asn.codecs.AsnSequenceOfSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnUintCodec;
import org.interledger.encoding.asn.framework.AsnObjectSerializationContext;
import org.interledger.encoding.asn.framework.AsnObjectSerializer;
import org.interledger.encoding.asn.framework.CodecException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;

/**
 * An extension of {@link AsnObjectSerializer} for reading and writing an ASN.1 OER
 * object that is represented by an SEQUENCE.
 */
public class AsnSequenceOfSequenceOerSerializer
    implements AsnObjectSerializer<AsnSequenceOfSequenceCodec> {

  @Override
  public void read(AsnObjectSerializationContext context, AsnSequenceOfSequenceCodec instance,
                   InputStream inputStream)
      throws IOException {

    AsnUintCodec quantityCodec = new AsnUintCodec();
    context.read(quantityCodec, inputStream);

    BigInteger quantityBigInt = quantityCodec.decode();
    if (quantityBigInt.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
      throw new CodecException("SEQUENCE_OF quantities > Integer.MAX_VALUE ar not supported");
    }

    int quantity = quantityBigInt.intValue();
    instance.setSize(quantity);

    for (int i = 0; i < quantity; i++) {
      context.read(instance.getCodecAt(i), inputStream);
    }
  }

  @Override
  public void write(AsnObjectSerializationContext context,
                    AsnSequenceOfSequenceCodec instance,
                    OutputStream outputStream) throws IOException {

    AsnUintCodec quantityCodec = new AsnUintCodec();
    quantityCodec.encode(BigInteger.valueOf(instance.size()));
    context.write(quantityCodec, outputStream);

    for (int i = 0; i < instance.size(); i++) {
      context.write(instance.getCodecAt(i), outputStream);
    }

  }
}
