package org.interledger.encoding.asn.framework;

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
   * @return a codec for encoding/decoding objects of type {@link T}
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

