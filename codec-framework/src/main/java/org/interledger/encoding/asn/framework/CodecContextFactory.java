package org.interledger.encoding.asn.framework;

/*-
 * ========================LICENSE_START=================================
 * Interledger Codec Framework
 * %%
 * Copyright (C) 2017 - 2018 Hyperledger and its contributors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */

import static java.lang.String.format;

import org.interledger.encoding.asn.codecs.AsnCharStringBasedObjectCodec;
import org.interledger.encoding.asn.codecs.AsnIA5StringBasedObjectCodec;
import org.interledger.encoding.asn.codecs.AsnIA5StringCodec;
import org.interledger.encoding.asn.codecs.AsnOctetStringBasedObjectCodec;
import org.interledger.encoding.asn.codecs.AsnOctetStringCodec;
import org.interledger.encoding.asn.codecs.AsnOpenTypeCodec;
import org.interledger.encoding.asn.codecs.AsnSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnSequenceOfSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;
import org.interledger.encoding.asn.codecs.AsnUint16Codec;
import org.interledger.encoding.asn.codecs.AsnUint32Codec;
import org.interledger.encoding.asn.codecs.AsnUint64Codec;
import org.interledger.encoding.asn.codecs.AsnUint8Codec;
import org.interledger.encoding.asn.codecs.AsnUtf8StringBasedObjectCodec;
import org.interledger.encoding.asn.codecs.AsnUtf8StringCodec;
import org.interledger.encoding.asn.serializers.oer.AsnCharStringOerSerializer;
import org.interledger.encoding.asn.serializers.oer.AsnOctetStringOerSerializer;
import org.interledger.encoding.asn.serializers.oer.AsnOpenTypeOerSerializer;
import org.interledger.encoding.asn.serializers.oer.AsnSequenceOerSerializer;
import org.interledger.encoding.asn.serializers.oer.AsnSequenceOfSequenceOerSerializer;

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
   *
   * @return a new {@link CodecContext}
   */
  public static CodecContext getContext(String encodingRules) {

    AsnObjectCodecRegistry mappings = new AsnObjectCodecRegistry()
        .register(byte[].class, () -> new AsnOctetStringCodec(AsnSizeConstraint.UNCONSTRAINED))
        .register(Short.class, AsnUint8Codec::new)
        .register(Integer.class, AsnUint16Codec::new)
        .register(Long.class, AsnUint32Codec::new)
        .register(BigInteger.class, AsnUint64Codec::new)
        .register(String.class, () -> new AsnUtf8StringCodec(AsnSizeConstraint.UNCONSTRAINED));

    AsnObjectSerializationContext serializers = new AsnObjectSerializationContext();

    if (OCTET_ENCODING_RULES.equals(encodingRules)) {
      serializers
          .register(AsnCharStringBasedObjectCodec.class, new AsnCharStringOerSerializer())
          .register(AsnIA5StringCodec.class, new AsnCharStringOerSerializer())
          .register(AsnIA5StringBasedObjectCodec.class, new AsnCharStringOerSerializer())
          .register(AsnOctetStringCodec.class, new AsnOctetStringOerSerializer())
          .register(AsnOpenTypeCodec.class, new AsnOpenTypeOerSerializer())
          .register(AsnOctetStringBasedObjectCodec.class, new AsnOctetStringOerSerializer())
          .register(AsnSequenceCodec.class, new AsnSequenceOerSerializer())
          .register(AsnSequenceOfSequenceCodec.class, new AsnSequenceOfSequenceOerSerializer())
          .register(AsnUint8Codec.class, new AsnOctetStringOerSerializer())
          .register(AsnUint16Codec.class, new AsnOctetStringOerSerializer())
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
