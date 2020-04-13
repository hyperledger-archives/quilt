package org.interledger.encoding.asn.serializers.oer;

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
 * An extension of {@link AsnObjectSerializer} for reading and writing an ASN.1 OER object that is represented by a
 * SEQUENCE OF SEQUENCE (which in ASN.1 OER nomenclature is an array of objects).
 */
public class AsnSequenceOfSequenceOerSerializer implements AsnObjectSerializer<AsnSequenceOfSequenceCodec> {

  @Override
  public void read(
      final AsnObjectSerializationContext context,
      final AsnSequenceOfSequenceCodec instance,
      final InputStream inputStream
  ) throws IOException {

    AsnUintCodec quantityCodec = new AsnUintCodec();
    context.read(quantityCodec, inputStream);

    BigInteger quantityBigInt = quantityCodec.decode();
    if (quantityBigInt.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
      throw new CodecException("SEQUENCE_OF quantities > Integer.MAX_VALUE ar not supported");
    }

    // This is the number of sequences that were indicated in the ASN.1 OER encoding. While this is specified by an
    // outside caller and _could_ be malicious, the code will not read past the actual number of bytes in the
    // InputStream, which due to ILPv4 packet limits should never be greater than 32kb.
    int indicatedNumberOfSequences = quantityBigInt.intValue();
    for (int i = 0; i < indicatedNumberOfSequences; i++) {
      context.read(instance.getCodecAt(i), inputStream);
    }
  }

  @Override
  public void write(
      final AsnObjectSerializationContext context,
      final AsnSequenceOfSequenceCodec instance,
      final OutputStream outputStream
  ) throws IOException {

    AsnUintCodec quantityCodec = new AsnUintCodec();
    quantityCodec.encode(BigInteger.valueOf(instance.size()));
    context.write(quantityCodec, outputStream);

    for (int i = 0; i < instance.size(); i++) {
      context.write(instance.getCodecAt(i), outputStream);
    }

  }
}
