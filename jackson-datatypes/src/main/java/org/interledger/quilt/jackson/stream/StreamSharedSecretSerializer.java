package org.interledger.quilt.jackson.stream;

/*-
 * ========================LICENSE_START=================================
 * Interledger Jackson Datatypes
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

import org.interledger.stream.crypto.StreamSharedSecret;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

import java.io.IOException;

/**
 * Jackson Serializer for {@link StreamSharedSecret}.
 */
public class StreamSharedSecretSerializer extends StdScalarSerializer<StreamSharedSecret> {

  public static final StreamSharedSecretSerializer INSTANCE = new StreamSharedSecretSerializer();

  /**
   * No-args Constructor.
   */
  public StreamSharedSecretSerializer() {
    super(StreamSharedSecret.class, false);
  }

  @Override
  public void serialize(StreamSharedSecret value, JsonGenerator gen, SerializerProvider provider) throws IOException {
    gen.writeString(value.value());
  }
}
