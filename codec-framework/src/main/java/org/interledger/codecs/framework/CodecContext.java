package org.interledger.codecs.framework;

import org.interledger.codecs.asn.AsnObject;
import org.interledger.codecs.asn.AsnObjectSupplier;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.function.Supplier;

public class CodecContext {

  private final AsnObjectMappingContext mappings;
  private final SerializationContext serializers;

  public CodecContext(AsnObjectMappingContext mappings, SerializationContext serializers) {
    this.mappings = mappings;
    this.serializers = serializers;
  }

  /**
   * Register a mapping between an object and an {@link AsnObject} supplier that can be used to
   * encode/decode the object and it's ASN.1 representation.
   *
   * @param type      The type of object that will be encoded/decoded and serialized/deserialized
   * @param supplier  A {@link Supplier} that creates empty ASN.1 wrappers for use by the serializer
   *
   * @return this object so that calls to register can be chained together.
   *
   * @throws CodecException if there is no serializer available for the given {@link AsnObject} that
   * is provided by the {@code supplier}.
   */
  public <T> CodecContext register(Class<T> type, AsnObjectSupplier<T> supplier) {
    Objects.requireNonNull(type);
    Objects.requireNonNull(supplier);

    //Verify we have a serializer for this object (throws if not)
    serializers.lookup(supplier.get());

    //Register the mapping
    mappings.register(type, supplier);

    return this;
  }

  public <T> CodecContext register(Class<T> type, AsnObjectSupplier<T> supplier, AsnObjectSerializer serializer) {
    Objects.requireNonNull(type);
    Objects.requireNonNull(supplier);
    Objects.requireNonNull(serializer);

    //Register the serializer
    serializers.register(supplier.get().getClass(), serializer);

    //Register the mapping
    mappings.register(type, supplier);

    return this;
  }

  public <T> T read(Class<T> type, InputStream inputStream) throws IOException {
    AsnObject<T> asnObject1 = mappings.getAsnObjectForType(type);
    serializers.read(asnObject1, inputStream);
    return asnObject1.getValue();
  }

  public <T> void write(T instance, OutputStream outputStream) throws IOException {
    AsnObject<T> asnObject = mappings.getAsnObjectForType((Class<T>) instance.getClass());
    asnObject.setValue(instance);
    serializers.write(asnObject, outputStream);
  }

}
