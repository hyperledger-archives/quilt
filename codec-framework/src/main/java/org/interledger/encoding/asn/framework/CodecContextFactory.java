package org.interledger.encoding.asn.framework;

import static java.lang.String.format;

import org.interledger.encoding.asn.codecs.AsnCharStringBasedObjectCodec;
import org.interledger.encoding.asn.codecs.AsnIA5StringBasedObjectCodec;
import org.interledger.encoding.asn.codecs.AsnIA5StringCodec;
import org.interledger.encoding.asn.codecs.AsnOctetStringBasedObjectCodec;
import org.interledger.encoding.asn.codecs.AsnOctetStringCodec;
import org.interledger.encoding.asn.codecs.AsnSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;
import org.interledger.encoding.asn.codecs.AsnUint32Codec;
import org.interledger.encoding.asn.codecs.AsnUint64Codec;
import org.interledger.encoding.asn.codecs.AsnUint8Codec;
import org.interledger.encoding.asn.codecs.AsnUtf8StringBasedObjectCodec;
import org.interledger.encoding.asn.codecs.AsnUtf8StringCodec;
import org.interledger.encoding.asn.serializers.oer.AsnCharStringOerSerializer;
import org.interledger.encoding.asn.serializers.oer.AsnOctetStringOerSerializer;
import org.interledger.encoding.asn.serializers.oer.AsnSequenceOerSerializer;
import org.interledger.encoding.asn.serializers.oer.AsnUint8OerSerializer;

import java.math.BigInteger;

/**
 * A factory for the standard ASN.1 types and built-in serializers
 */
public class CodecContextFactory {

  public static final String OCTET_ENCODING_RULES = "OER";

  /**
   * Get the {@link CodecContext} loaded with neccessary codecs and serializers for the given
   * encoding rules.
   *
   * @param encodingRules encoding rules to use for serialization
   * @return a new {@link CodecContext}
   */
  public static CodecContext getContext(String encodingRules) {

    AsnObjectCodecRegistry mappings = new AsnObjectCodecRegistry()
        .register(byte[].class, () -> new AsnOctetStringCodec(AsnSizeConstraint.UNCONSTRAINED))
        .register(Integer.class, AsnUint8Codec::new)
        .register(Long.class, AsnUint64Codec::new)
        .register(String.class, () -> new AsnUtf8StringCodec(AsnSizeConstraint.UNCONSTRAINED));

    AsnObjectSerializationContext serializers = new AsnObjectSerializationContext();

    if (OCTET_ENCODING_RULES.equals(encodingRules)) {
      serializers
          .register(AsnCharStringBasedObjectCodec.class, new AsnCharStringOerSerializer())
          .register(AsnIA5StringCodec.class, new AsnCharStringOerSerializer())
          .register(AsnIA5StringBasedObjectCodec.class, new AsnCharStringOerSerializer())
          .register(AsnOctetStringCodec.class, new AsnOctetStringOerSerializer())
          .register(AsnOctetStringBasedObjectCodec.class, new AsnOctetStringOerSerializer())
          .register(AsnSequenceCodec.class, new AsnSequenceOerSerializer())
          .register(AsnUint8Codec.class, new AsnUint8OerSerializer())
          .register(AsnUint32Codec.class, new AsnOctetStringOerSerializer())
          .register(AsnUint64Codec.class, new AsnOctetStringOerSerializer())
          .register(AsnUtf8StringCodec.class, new AsnCharStringOerSerializer())
          .register(AsnUtf8StringBasedObjectCodec.class, new AsnCharStringOerSerializer());

    } else {
      throw new CodecException(format("Unknown encoding rules '%s'", encodingRules));
    }

    return new CodecContext(mappings, serializers);

  }


}
