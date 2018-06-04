package org.interledger.encoding.asn.serializers.oer;

/*-
 * ========================LICENSE_START=================================
 * Interledger Codec Framework
 * %%
 * Copyright (C) 2017 - 2018 Interledger
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

import org.interledger.encoding.asn.codecs.AsnUint8Codec;
import org.interledger.encoding.asn.framework.AsnObjectSerializationContext;
import org.interledger.encoding.asn.framework.AsnObjectSerializer;
import org.interledger.encoding.asn.framework.CodecException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * An extension of {@link AsnObjectSerializer} for reading and writing an ASN.1 OER 8-Bit
 * integer.
 *
 * <p>This serializer is used instead of the generic Octet String serializer for performance
 * reasons.
 */
public class AsnUint8OerSerializer implements AsnObjectSerializer<AsnUint8Codec> {

  @Override
  public void read(final AsnObjectSerializationContext context, final AsnUint8Codec instance,
                   final InputStream
      inputStream)
      throws IOException {
    Objects.requireNonNull(context);
    Objects.requireNonNull(instance);
    Objects.requireNonNull(inputStream);

    int value = inputStream.read();
    if (value == -1) {
      throw new CodecException("Unexpected end of stream.");
    }

    instance.encode(value);

  }

  @Override
  public void write(AsnObjectSerializationContext context, AsnUint8Codec instance,
                    OutputStream outputStream)
      throws IOException {

    if (instance.decode() > 255) {
      throw new IllegalArgumentException("UInt8 values may only contain up to 8 bits!");
    }

    outputStream.write(instance.decode());
  }

}
