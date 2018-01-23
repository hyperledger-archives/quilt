package org.interledger.codecs.framework;

import static java.lang.String.format;

import org.interledger.codecs.asn.AsnCharString;
import org.interledger.codecs.asn.AsnCharStringBasedObject;
import org.interledger.codecs.asn.AsnGeneralizedTime;
import org.interledger.codecs.asn.AsnIA5String;
import org.interledger.codecs.asn.AsnOctetString;
import org.interledger.codecs.asn.AsnOctetStringBasedObject;
import org.interledger.codecs.asn.AsnSequence;
import org.interledger.codecs.asn.AsnSizeConstraint;
import org.interledger.codecs.asn.AsnUTF8String;
import org.interledger.codecs.asn.AsnUint32;
import org.interledger.codecs.asn.AsnUint64;
import org.interledger.codecs.asn.AsnUint8;
import org.interledger.codecs.oer.AsnCharStringOerSerializer;
import org.interledger.codecs.oer.AsnOctetStringOerSerializer;
import org.interledger.codecs.oer.AsnSequenceOerSerializer;
import org.interledger.codecs.oer.AsnUint8OerSerializer;

import java.math.BigInteger;
import java.time.Instant;

public class CodecContextFactory {

  public static final String OCTET_ENCODING_RULES = "OER";

  public static CodecContext getContext(String encodingRules) {

    AsnObjectMappingContext mappings = new AsnObjectMappingContext()
        .register(Instant.class, () -> new AsnGeneralizedTime())
        .register(byte[].class, () -> new AsnOctetString(AsnSizeConstraint.unconstrained()))
        .register(Integer.class, () -> new AsnUint8())
        .register(Long.class, () -> new AsnUint32())
        .register(BigInteger.class, () -> new AsnUint64())
        .register(String.class, () -> new AsnUTF8String(AsnSizeConstraint.unconstrained()));

    SerializationContext serializers = new SerializationContext();

    if (OCTET_ENCODING_RULES.equals(encodingRules)) {
      serializers
          .register(AsnCharString.class, new AsnCharStringOerSerializer())
          .register(AsnCharStringBasedObject.class, new AsnCharStringOerSerializer())
          .register(AsnGeneralizedTime.class, new AsnCharStringOerSerializer())
          .register(AsnIA5String.class, new AsnCharStringOerSerializer())
          .register(AsnOctetString.class, new AsnOctetStringOerSerializer())
          .register(AsnOctetStringBasedObject.class, new AsnOctetStringOerSerializer())
          .register(AsnSequence.class, new AsnSequenceOerSerializer())
          .register(AsnUint8.class, new AsnUint8OerSerializer())
          .register(AsnUint32.class, new AsnOctetStringOerSerializer())
          .register(AsnUint64.class, new AsnOctetStringOerSerializer())
          .register(AsnUTF8String.class, new AsnCharStringOerSerializer());

    } else {
      throw new CodecException(format("Unknown encoding rules '%s'", encodingRules));
    }

    return new CodecContext(mappings, serializers);

  }


}
