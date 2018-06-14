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
