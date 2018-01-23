package org.interledger.codecs.framework;

import org.interledger.codecs.asn.AsnObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A contextual object for matching instances of {@link AsnObject} to specific serializers.
 */
public class SerializationContext {

  /**
   * A map of serializers that can serialize/deserialize based on an ASN.1 object class type.
   */
  private final Map<Class<? extends AsnObject>, AsnObjectSerializer> serializers;

  /**
   * No-args Constructor.
   */
  public SerializationContext() {
    this.serializers = new ConcurrentHashMap<>();
  }

  /**
   * Register a converter associated to the supplied {@code type}.
   *
   * @param type       An instance of {@link Class}.
   * @param serializer An instance of {@link AsnObjectSerializer}.
   * @return A {@link SerializationContext} for the supplied {@code type}.
   */
  public SerializationContext register(final Class<? extends AsnObject> type,
                                       final AsnObjectSerializer serializer) {
    Objects.requireNonNull(type);
    Objects.requireNonNull(serializer);

    this.serializers.put(type, serializer);
    return this;
  }

  /**
   * Helper method that accepts an {@link InputStream} and a type hint, and then decodes the input
   * to the appropriate response payload.
   *
   * @param instance    An instance of {@link AsnObject} that will be populated with the
   *                    deserialized data.
   * @param inputStream An instance of {@link InputStream} that contains bytes in a certain
   *                    encoding.
   * @throws IOException If anything goes wrong reading from the {@code buffer}.
   */
  public SerializationContext read(final AsnObject instance, final InputStream inputStream)
      throws IOException {
    Objects.requireNonNull(instance);
    Objects.requireNonNull(inputStream);
    lookup(instance).read(this, instance, inputStream);
    return this;
  }

  /**
   * Helper method that accepts a byte array and a type hint, and then decodes the input to the
   * appropriate response payload.
   * <p>
   * <p>NOTE: This methods wraps IOExceptions in RuntimeExceptions.
   *
   * @param data An instance of byte array that contains bytes in a certain encoding.
   */
  public SerializationContext read(final AsnObject instance, final byte[] data) {
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
   * Writes an instance of {@code {@link AsnObject}} to the supplied {@link OutputStream}.
   *
   * @param instance     An instance of {@link Object} that will be encoded to the output stream.
   * @param outputStream An instance of {@link OutputStream} that will be written to.
   * @return An instance of {@link SerializationContext} for further operations.
   * @throws IOException If anything goes wrong while writing to the {@link OutputStream}
   */
  public SerializationContext write(final AsnObject instance, final OutputStream outputStream)
      throws IOException {
    Objects.requireNonNull(instance);
    Objects.requireNonNull(outputStream);

    lookup(instance).write(this, instance, outputStream);

    return this;
  }

  /**
   * Writes an instance of {@code instance} to an in-memory stream and returns the result as a byte
   * array.
   * <p>
   * <p>NOTE: This methods wraps any IOExceptions in a RuntimeException.
   *
   * @param instance An instance of {@link AsnObject} that will be encoded to the output stream.
   * @return The encoded object.
   */
  public byte[] write(final AsnObject instance) {
    Objects.requireNonNull(instance);

    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      lookup(instance).write(this, instance, baos);
      return baos.toByteArray();
    } catch (IOException e) {
      throw new CodecException("Error encoding " + instance.getClass().getCanonicalName(), e);
    }
  }

  /**
   * Helper method to lookup a {@link AsnObjectSerializer} for the specified {@link AsnObject}.
   *
   * @param instance An instance of {@link AsnObject}.
   *
   * @return an {@link AsnObjectSerializer} for serializing this object.
   * @throws CodecException if there is no serializer registered.
   *
   */
  public AsnObjectSerializer lookup(final AsnObject instance) {
    Objects.requireNonNull(instance);
    Class type = instance.getClass();

    if (serializers.containsKey(type)) {
      return serializers.get(type);
    } else {
      // Check any interfaces...
      return
          Optional.of(
              Arrays.stream(type.getInterfaces())
                  // Only use serializers that are found...
                  .filter(serializers::containsKey)
                  // If found, map to the first codec...
                  .map(interfaceClass -> serializers.get(interfaceClass))
                  .findFirst()
                  .orElseGet(() -> {
                    // There were not interfaces, so check the super-classes.
                    if (type.getSuperclass() != null &&
                        serializers.containsKey(type.getSuperclass())) {
                      return serializers.get(type.getSuperclass());
                    }
                    return null;
                  }))
              .orElseThrow(() -> new CodecException(
                  String.format("No serializer registered for %s or its interfaces or super classes!",
                      type.getName())));
    }
  }

}
