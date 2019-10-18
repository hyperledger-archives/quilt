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
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A context that maps native objects to their ASN.1 codecs and the codecs to serializers.
 */
public class CodecContext {

  private final AsnObjectCodecRegistry mappings;
  private final AsnObjectSerializationContext serializers;

  public CodecContext(AsnObjectCodecRegistry mappings, AsnObjectSerializationContext serializers) {
    this.mappings = mappings;
    this.serializers = serializers;
  }

  /**
   * Register a mapping between an object and an {@link AsnObjectCodec} supplier that can be used to
   * encode/decode the object into it's ASN.1 representation.
   *
   * @param type     The type of object that will be encoded/decoded and serialized/deserialized
   * @param supplier A {@link Supplier} that creates new ASN.1 codecs for use by the serializer
   * @param <T>      The type of object that will be encoded/decoded and serialized/deserialized
   *
   * @return this object so that calls to register can be chained together.
   * @throws CodecException if there is no serializer available for the given {@link AsnObjectCodec}
   *                        that is provided by the {@code supplier}.
   */
  public <T> CodecContext register(Class<T> type, AsnObjectCodecSupplier<T> supplier) {
    Objects.requireNonNull(type);
    Objects.requireNonNull(supplier);

    //Verify we have a serializer for this object (throws if not)
    serializers.getSerializer(supplier.get());

    //Register the mapping
    mappings.register(type, supplier);

    return this;
  }

  /**
   * Register a mapping between an object and an {@link AsnObjectCodec} supplier that can be used to
   * encode/decode the object into it's ASN.1 representation and also register the serializer that
   * is to be used for serializing these objects.
   *
   * @param type       The type of the objects that must be encoded/decoded and serialized by the
   *                   given ASN.1 encoding and serializers.
   * @param supplier   A supplier of new ASN.1 codecs that can be used to encode/decode objects
   *                   of the given type.
   * @param serializer A serializer that can serialize and deserialize the given ASN.1 object
   * @param <T>        The type of object being registered.
   * @param <U>        The type of ASN.1 codec being registered.
   * @return this for chaining.
   */
  @SuppressWarnings("unchecked")
  public <T, U extends AsnObjectCodec<T>> CodecContext register(Class<T> type,
                                                                AsnObjectCodecSupplier<T> supplier,
                                                                AsnObjectSerializer<U> serializer) {
    Objects.requireNonNull(type);
    Objects.requireNonNull(supplier);
    Objects.requireNonNull(serializer);

    //Register the serializer
    serializers.register((Class<U>) ((U) supplier.get()).getClass(), serializer);

    //Register the mapping
    mappings.register(type, supplier);

    return this;
  }

  /**
   * Deserialize an object of the given type from the given stream and use the appropriate ASN.1
   * codec to decode the deserialized ASN.1 object into an object of type {@code T}.
   *
   * @param type        The type of the object to read from the stream.
   * @param inputStream The stream from which to read the object.
   * @param <T>         The type of the object.
   * @return the object read from the stream.
   * @throws IOException if there are errors reading from the stream.
   */
  public <T> T read(Class<T> type, InputStream inputStream) throws IOException {
    AsnObjectCodec<T> asnObjectCodec = mappings.getAsnObjectForType(type);
    serializers.read(asnObjectCodec, inputStream);
    return asnObjectCodec.decode();
  }

  /**
   * Encode the given object to it's ASN.1 form and then serialize to the given stream.
   *
   * @param instance     The object to write.
   * @param outputStream The stream to write to.
   * @param <T>          The type of the object.
   * @throws IOException if there are errors writing to the stream.
   */
  public <T> void write(T instance, OutputStream outputStream) throws IOException {
    AsnObjectCodec<T> asnObjectCodec = mappings.getAsnObjectForType((Class<T>) instance
        .getClass());
    asnObjectCodec.encode(instance);
    serializers.write(asnObjectCodec, outputStream);
  }

}
