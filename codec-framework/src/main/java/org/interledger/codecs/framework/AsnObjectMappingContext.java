package org.interledger.codecs.framework;

import org.interledger.codecs.asn.AsnObject;
import org.interledger.codecs.asn.AsnObjectSupplier;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A contextual object that supplies {@link AsnObject} instances for a given native type.
 */
public class AsnObjectMappingContext {

  /**
   * A map of codecContext that can encode/decode based on a class type. This is for
   * encoding/decoding
   * objects that are part of a known packet layout.
   */
  private final Map<Class<?>, AsnObjectSupplier> mappersByObjectType;

  /**
   * No-args Constructor.
   */
  public AsnObjectMappingContext() {
    this.mappersByObjectType = new ConcurrentHashMap<>();
  }

  /**
   * Register a.
   *
   * @param type An instance of {@link Class}.
   * @return A {@link AsnObjectMappingContext} for the supplied {@code type}.
   * @throws {@link CodecException} if there is no codec registered for the type of
   */
  public <T> AsnObjectMappingContext register(Class<T> type, AsnObjectSupplier<T> supplier) {
    Objects.requireNonNull(type);
    Objects.requireNonNull(supplier);

    this.mappersByObjectType.put(type, supplier);

    return this;
  }

//  public AsnObject<T> getAsnObjectForInstance(final T instance) {
//    AsnObject<T> asnObject = getAsnObjectForType(instance.getClass());
//    asnObject.setValue(instance);
//    return asnObject;
//  }

  /**
   * Method to get a new {@link AsnObject} for the specified {@code type}.
   *
   * @param type An instance of {@link Class}.
   */
  public <T> AsnObject<T> getAsnObjectForType(final Class<T> type) {
    Objects.requireNonNull(type);

    AsnObject<T> asnObj = tryGetAsnObjectForType(type);
    if (asnObj == null) {
      throw new CodecException(
          String.format("No codec registered for %s or its super classes!",
              type.getName()));
    }
    return asnObj;
  }

  private <T> AsnObject<T> tryGetAsnObjectForType(final Class<T> type) {
    Objects.requireNonNull(type);

    AsnObject<T> asnObj;

    if (mappersByObjectType.containsKey(type)) {
      asnObj = mappersByObjectType.get(type).get();
      if (asnObj != null) {
        return asnObj;
      }
    }

    if (type.getSuperclass() != null) {
      asnObj = (AsnObject<T>) tryGetAsnObjectForType(type.getSuperclass());
      if (asnObj != null) {
        return asnObj;
      }
    }

    for (Class<?> interfaceType : type.getInterfaces()) {
      asnObj = (AsnObject<T>) tryGetAsnObjectForType(interfaceType);
      if (asnObj != null) {
        return asnObj;
      }
    }

    return null;
  }

}

