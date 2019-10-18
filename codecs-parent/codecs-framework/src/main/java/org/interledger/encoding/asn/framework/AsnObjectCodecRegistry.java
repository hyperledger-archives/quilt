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

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A registry of {@link Class} to {@link AsnObjectCodec} mappings that can provide a new instance of
 * an {@link AsnObjectCodec} that can encode/decode a given type.
 */
public class AsnObjectCodecRegistry {

  private final Map<Class<?>, AsnObjectCodecSupplier> mappersByObjectType;

  /**
   * No-args Constructor.
   */
  public AsnObjectCodecRegistry() {
    this.mappersByObjectType = new ConcurrentHashMap<>();
  }

  /**
   * Register a new ASN.1 codec that can encode/decode the given type.
   *
   * @param type     An instance of {@link Class}.
   * @param supplier The {@link AsnObjectCodecSupplier} that will produce new
   *                 {@link AsnObjectCodec} instances as required.
   * @param <T>      The type of object that can be encoded/decoded by the given codec.
   * @return this {@link AsnObjectCodecRegistry} to allow chaining calls to this method.
   */
  public <T> AsnObjectCodecRegistry register(Class<T> type, AsnObjectCodecSupplier<T> supplier) {
    Objects.requireNonNull(type);
    Objects.requireNonNull(supplier);

    this.mappersByObjectType.put(type, supplier);

    return this;
  }

  /**
   * Get a new {@link AsnObjectCodec} instance to encode/decode the specified {@code type}.
   *
   * @param type An instance of {@link Class}.
   * @param <T> the type of object that can be encoded/decoded by the returned codec
   *
   * @return a codec for encoding/decoding objects of type {@code T}
   */
  public <T> AsnObjectCodec<T> getAsnObjectForType(final Class<T> type) {
    Objects.requireNonNull(type);

    AsnObjectCodec<T> codec = tryGetAsnObjectForType(type);
    if (codec == null) {
      throw new CodecException(
          String.format("No codec registered for %s or its super classes!",
              type.getName()));
    }
    return codec;
  }

  private <T> AsnObjectCodec<T> tryGetAsnObjectForType(final Class<T> type) {
    Objects.requireNonNull(type);

    AsnObjectCodec<T> codec;

    if (mappersByObjectType.containsKey(type)) {
      codec = mappersByObjectType.get(type).get();
      if (codec != null) {
        return codec;
      }
    }

    if (type.getSuperclass() != null) {
      codec = (AsnObjectCodec<T>) tryGetAsnObjectForType(type.getSuperclass());
      if (codec != null) {
        return codec;
      }
    }

    for (Class<?> interfaceType : type.getInterfaces()) {
      codec = (AsnObjectCodec<T>) tryGetAsnObjectForType(interfaceType);
      if (codec != null) {
        return codec;
      }
    }

    return null;
  }

}

