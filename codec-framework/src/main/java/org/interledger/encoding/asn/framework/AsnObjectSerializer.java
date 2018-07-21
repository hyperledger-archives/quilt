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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A serializer/deserializer interface for ASN.1 objects.
 */
public interface AsnObjectSerializer<T extends AsnObjectCodec> {

  /**
   * Read an ASN.1 object from the buffer according to the rules defined in the {@link
   * AsnObjectSerializationContext}.
   *
   * @param context     An instance of {@link AsnObjectSerializationContext}.
   * @param instance    An instance of {@link AsnObjectCodec} to read the data into.
   * @param inputStream An instance of {@link InputStream} to read data from.
   *
   * @throws IOException if anything goes wrong reading from the {@link InputStream}.
   */
  void read(AsnObjectSerializationContext context, T instance, InputStream inputStream)
      throws IOException;

  /**
   * Write an object to the {@code outputStream} according to the rules defined in the {@code
   * context}.
   *
   * @param context      An instance of {@link AsnObjectSerializationContext}.
   * @param instance     An instance of {@link AsnObjectCodec} that is being serialized.
   * @param outputStream An instance of {@link OutputStream} to write data to.
   *
   * @throws IOException If anything goes wrong writing to the {@link OutputStream}
   */
  void write(AsnObjectSerializationContext context, T instance, OutputStream outputStream)
      throws IOException;

}
