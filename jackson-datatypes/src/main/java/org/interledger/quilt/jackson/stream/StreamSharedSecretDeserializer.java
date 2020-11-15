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

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;

/**
 * An extension of {@link FromStringDeserializer} that deserializes a JSON string into an instance of {@link
 * StreamSharedSecret}.
 */
public class StreamSharedSecretDeserializer extends FromStringDeserializer<StreamSharedSecret> {

  public static final StreamSharedSecretDeserializer INSTANCE = new StreamSharedSecretDeserializer();

  /**
   * No-args Constructor.
   */
  public StreamSharedSecretDeserializer() {
    super(StreamSharedSecret.class);
  }

  @Override
  protected StreamSharedSecret _deserialize(
    final String value, final DeserializationContext deserializationContext
  ) {
    return StreamSharedSecret.of(value);
  }
}
