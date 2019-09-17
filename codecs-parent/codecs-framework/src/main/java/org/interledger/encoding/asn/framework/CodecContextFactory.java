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
import org.interledger.encoding.asn.codecs.AsnUint64CodecUL;
import org.interledger.encoding.asn.codecs.AsnUint8Codec;
import org.interledger.encoding.asn.codecs.AsnUintCodec;
import org.interledger.encoding.asn.codecs.AsnUintCodecUL;
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

  /**
   * Constructs a {@link CodecContext} that is configured to read and write ASN.1 OER encodings.
   *
   * @return A {@link CodecContext}
   */
  public static CodecContext oer() {
    final AsnObjectCodecRegistry mappings = new AsnObjectCodecRegistry()
        .register(byte[].class, () -> new AsnOctetStringCodec(AsnSizeConstraint.UNCONSTRAINED))
        .register(Short.class, AsnUint8Codec::new) // unsigned!
        .register(Integer.class, AsnUint16Codec::new)
        .register(Long.class, AsnUint32Codec::new) // Make this UnsignedInt or just Int
        .register(BigInteger.class,
            AsnUint64Codec::new) // Uint64 should be UnsignedLong or just Long, then AsnUint64 can be BigInteger.
        .register(String.class, () -> new AsnUtf8StringCodec(AsnSizeConstraint.UNCONSTRAINED));

    final AsnObjectSerializationContext serializers = new AsnObjectSerializationContext()
        .register(AsnCharStringBasedObjectCodec.class, new AsnCharStringOerSerializer())
        .register(AsnIA5StringCodec.class, new AsnCharStringOerSerializer())
        .register(AsnIA5StringBasedObjectCodec.class, new AsnCharStringOerSerializer())
        .register(AsnOctetStringCodec.class, new AsnOctetStringOerSerializer())
        .register(AsnOpenTypeCodec.class, new AsnOpenTypeOerSerializer())
        .register(AsnOctetStringBasedObjectCodec.class, new AsnOctetStringOerSerializer())
        .register(AsnSequenceCodec.class, new AsnSequenceOerSerializer())
        .register(AsnSequenceOfSequenceCodec.class, new AsnSequenceOfSequenceOerSerializer())
        .register(AsnUintCodec.class, new AsnOctetStringOerSerializer())
        .register(AsnUintCodecUL.class, new AsnOctetStringOerSerializer())
        .register(AsnUint8Codec.class, new AsnOctetStringOerSerializer())
        .register(AsnUint16Codec.class, new AsnOctetStringOerSerializer())
        .register(AsnUint32Codec.class, new AsnOctetStringOerSerializer())
        .register(AsnUint64Codec.class, new AsnOctetStringOerSerializer())
        .register(AsnUint64CodecUL.class, new AsnOctetStringOerSerializer())
        .register(AsnUtf8StringCodec.class, new AsnCharStringOerSerializer())
        .register(AsnUtf8StringBasedObjectCodec.class, new AsnCharStringOerSerializer());

    return new CodecContext(mappings, serializers);
  }
}
