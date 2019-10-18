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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A contextual object for serializing {@link AsnObjectCodec} using registered serializers.
 */
public class AsnObjectSerializationContext {

  /**
   * A map of serializers that can serialize/deserialize based on an ASN.1 object class type.
   */
  private final Map<Class<? extends AsnObjectCodec>, AsnObjectSerializer> serializers;

  /**
   * No-args Constructor.
   */
  public AsnObjectSerializationContext() {
    this.serializers = new ConcurrentHashMap<>();
  }

  /**
   * Register a serializer associated to the supplied {@code type}.
   *
   * @param type       An instance of {@link Class}.
   * @param serializer An instance of {@link AsnObjectSerializer}.
   * @param <T>        The type of {@link AsnObjectCodec} that can be serialized by this
   *                   serializer.
   *
   * @return A {@link AsnObjectSerializationContext} for the supplied {@code type}.
   */
  public <T extends AsnObjectCodec> AsnObjectSerializationContext register(
      final Class<T> type,
      final AsnObjectSerializer<? super T> serializer) {

    Objects.requireNonNull(type);
    Objects.requireNonNull(serializer);

    this.serializers.put(type, serializer);
    return this;
  }

  /**
   * Read an deserialize an ASN.1 object from a stream.
   *
   * @param instance    An instance of {@link AsnObjectCodec} that will be populated with the
   *                    deserialized data.
   * @param inputStream An instance of {@link InputStream} that contains bytes in a certain
   *                    encoding.
   *
   * @return this {@link AsnObjectSerializationContext} for further operations.
   *
   * @throws IOException If anything goes wrong reading from the {@code buffer}.
   */
  public AsnObjectSerializationContext read(final AsnObjectCodec instance,
      final InputStream inputStream)
      throws IOException {
    Objects.requireNonNull(instance);
    Objects.requireNonNull(inputStream);
    getSerializer(instance).read(this, instance, inputStream);
    return this;
  }

  /**
   * Read an deserialize an ASN.1 object from a byte array.
   *
   * <p>NOTE: This methods wraps IOExceptions in RuntimeExceptions.
   *
   * @param instance An instance of {@link AsnObjectCodec} that will be populated with the
   *                 deserialized data.
   * @param data     An instance of byte array that contains bytes in a certain encoding.
   *
   * @return this {@link AsnObjectSerializationContext} for further operations.
   */
  public AsnObjectSerializationContext read(final AsnObjectCodec instance, final byte[] data) {
    Objects.requireNonNull(instance);
    Objects.requireNonNull(data);

    try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
      read(instance, bais);
    } catch (IOException e) {
      throw new CodecException("Unable to decode " + instance.getClass().getCanonicalName(), e);
    }

    return this;
  }

  /**
   * Write the decoded value of an {@link AsnObjectCodec} to the supplied {@link OutputStream}.
   *
   * @param instance     An instance of {@link AsnObjectCodec} that will encode the value to be
   *                     serialized.
   * @param outputStream An instance of {@link OutputStream} that will be written to.
   *
   * @return this {@link AsnObjectSerializationContext} for further operations.
   *
   * @throws IOException If anything goes wrong while writing to the {@link OutputStream}
   */
  public AsnObjectSerializationContext write(final AsnObjectCodec instance,
      final OutputStream outputStream)
      throws IOException {
    Objects.requireNonNull(instance);
    Objects.requireNonNull(outputStream);

    getSerializer(instance).write(this, instance, outputStream);

    return this;
  }

  /**
   * Write the decoded value of an {@link AsnObjectCodec} to an in-memory stream and return the
   * bytes.
   *
   * @param instance An instance of {@link AsnObjectCodec} that will encode the value to be
   *                 serialized.
   *
   * @return The serialized object.
   */
  public byte[] write(final AsnObjectCodec instance) {
    Objects.requireNonNull(instance);

    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      getSerializer(instance).write(this, instance, baos);
      return baos.toByteArray();
    } catch (IOException e) {
      throw new CodecException("Error encoding " + instance.getClass().getCanonicalName(), e);
    }
  }

  /**
   * Get the {@link AsnObjectSerializer} instance for serializing the given {@link AsnObjectCodec}.
   *
   * @param instance An instance of {@code T}.
   */
  <T extends AsnObjectCodec> AsnObjectSerializer<T> getSerializer(final T instance) {

    AsnObjectSerializer<T> serializer = tryGetSerializerForCodec((Class<T>) instance.getClass());

    if (serializer == null) {
      throw new CodecException(
          String.format("No serializer registered for %s or its super classes!",
              instance.getClass().getName()));
    }
    return serializer;
  }

  // Visible for testing...
  protected <T extends AsnObjectCodec> AsnObjectSerializer<T> tryGetSerializerForCodec(
      final Class<T> type
  ) {
    Objects.requireNonNull(type);

    AsnObjectSerializer<T> serializer;

    if (serializers.containsKey(type)) {
      serializer = serializers.get(type);
      if (serializer != null) {
        return serializer;
      }
    }

    if (type.getSuperclass() != null) {
      serializer = tryGetSerializerForCodec((Class<T>) type.getSuperclass());
      if (serializer != null) {
        return serializer;
      }
    }

    for (Class<?> interfaceType : type.getInterfaces()) {
      serializer = tryGetSerializerForCodec((Class<T>) interfaceType);
      if (serializer != null) {
        return serializer;
      }
    }

    return null;
  }

}
