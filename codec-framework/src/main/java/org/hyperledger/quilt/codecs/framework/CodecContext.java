package org.hyperledger.quilt.codecs.framework;

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
 * A contextual object for matching instances of {@link Codec} to specific class types.
 */
public class CodecContext {

  /**
   * A map of codecs that can encode/decode based on a class type. This is for encoding/decoding
   * objects that are part of a known packet layout.
   */
  private final Map<Class<?>, Codec<?>> codecs;

  /**
   * No-args Constructor.
   */
  public CodecContext() {
    this.codecs = new ConcurrentHashMap<>();
  }

  /**
   * Register a converter associated to the supplied {@code type}.
   *
   * @param type      An instance of {link Class} of type {@link T}.
   * @param converter An instance of {@link Codec}.
   * @param <T>       An instance of {@link T}.
   *
   * @return A {@link CodecContext} for the supplied {@code type}.
   */
  public <T> CodecContext register(final Class<? extends T> type, final Codec<T> converter) {
    Objects.requireNonNull(type);
    Objects.requireNonNull(converter);

    this.codecs.put(type, converter);
    return this;
  }

  /**
   * Helper method that accepts an {@link InputStream} and a type hint, and then decodes the input
   * to the appropriate response payload.
   *
   * @param type        An instance of {@link Class} that indicates the type that should be
   *                    decoded.
   * @param inputStream An instance of {@link InputStream} that contains bytes in a certain
   *                    encoding.
   * @param <T>         The type of object to return, based upon the supplied type of {@code type}.
   *
   * @return An instance of {@link T}.
   *
   * @throws IOException If anything goes wrong reading from the {@code buffer}.
   */
  public <T> T read(final Class<T> type, final InputStream inputStream) throws IOException {
    Objects.requireNonNull(type);
    Objects.requireNonNull(inputStream);

    return lookup(type).read(this, inputStream);
  }

  /**
   * Helper method that accepts a byte array and a type hint, and then decodes the input to the
   * appropriate response payload.
   *
   * <p>NOTE: This methods wraps IOExceptions in RuntimeExceptions.
   *
   * @param type An instance of {@link Class} that indicates the type that should be decoded.
   * @param data An instance of byte array that contains bytes in a certain encoding.
   * @param <T>  The type of object to return, based upon the supplied type of {@code type}.
   *
   * @return An instance of {@link T}.
   */
  public <T> T read(final Class<T> type, final byte[] data) {
    Objects.requireNonNull(type);
    Objects.requireNonNull(data);

    try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
      return lookup(type).read(this, bais);
    } catch (IOException e) {
      throw new CodecException("Unable to decode " + type.getCanonicalName(), e);
    }

  }

  /**
   * Writes an instance of {@code instance} to the supplied {@link OutputStream}.
   *
   * @param type         An instance of {@link Class} that indicates the type that should be
   *                     encoded.
   * @param instance     An instance of {@link T} that will be encoded to the output stream.
   * @param outputStream An instance of {@link OutputStream} that will be written to.
   * @param <T>          The type of object to encode.
   *
   * @return An instance of {@link CodecContext} for further operations.
   *
   * @throws IOException If anything goes wrong while writing to the {@link OutputStream}
   */
  public <T> CodecContext write(final Class<T> type, final T instance,
      final OutputStream outputStream) throws IOException {
    Objects.requireNonNull(type);
    Objects.requireNonNull(instance);
    Objects.requireNonNull(outputStream);

    lookup(type).write(this, instance, outputStream);
    return this;
  }

  /**
   * Writes a generic instance of {@code Object} to the supplied {@link OutputStream}.
   *
   * @param instance     An instance of {@link Object} that will be encoded to the output stream.
   * @param outputStream An instance of {@link OutputStream} that will be written to.
   *
   * @return An instance of {@link CodecContext} for further operations.
   *
   * @throws IOException If anything goes wrong while writing to the {@link OutputStream}
   */
  public CodecContext write(final Object instance, final OutputStream outputStream)
      throws IOException {
    Objects.requireNonNull(instance);
    Objects.requireNonNull(outputStream);

    lookup(instance.getClass()).writeObject(this, instance, outputStream);
    return this;
  }

  /**
   * Writes an instance of {@code instance} to an in-memory stream and returns the result as a byte
   * array.
   *
   * <p>NOTE: This methods wraps any IOExceptions in a RuntimeException.
   *
   * @param type     An instance of {@link Class} that indicates the type that should be encoded.
   * @param instance An instance of {@link T} that will be encoded to the output stream.
   * @param <T>      The type of object to encode.
   *
   * @return The encoded object.
   */
  public <T> byte[] write(final Class<T> type, final T instance) {
    Objects.requireNonNull(type);
    Objects.requireNonNull(instance);

    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      lookup(type).write(this, instance, baos);
      return baos.toByteArray();
    } catch (IOException e) {
      throw new CodecException("Error encoding " + type.getCanonicalName(), e);
    }
  }

  /**
   * Writes a generic instance of {@code Object} to an in-memory stream and returns the result as a
   * byte array.
   *
   * <p>NOTE: This methods wraps any IOExceptions in a RuntimeException.
   *
   * @param instance An instance of {@link Object} that will be encoded to the output stream.
   *
   * @return An instance of {@link CodecContext} for further operations.
   */
  public byte[] write(final Object instance) {
    Objects.requireNonNull(instance);

    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      lookup(instance.getClass()).writeObject(this, instance, baos);
      return baos.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException("Error encoding " + instance.getClass());
    }
  }

  /**
   * Helper method to lookup a {@link Codec} for the specified {@code type}.
   *
   * @param type An instance of {@link Class}.
   * @param <T>  The specific type of {@link Codec} to return.
   */
  @SuppressWarnings("unchecked")
  private <T> Codec<T> lookup(final Class<T> type) {
    Objects.requireNonNull(type);

    if (codecs.containsKey(type)) {
      return (Codec<T>) codecs.get(type);
    } else {
      // Check any interfaces...
      return Optional.ofNullable(
          Arrays.stream(type.getInterfaces())
              // Only use codecs that are found...
              .filter(codecs::containsKey)
              // If found, map to the first codec...
              .map(interfaceClass -> (Codec<T>) codecs.get(interfaceClass))
              .findFirst()
              // Otherwise, recurse with the super-class
              .orElseGet(() -> {
                // There were not interfaces, so check the super-classes.
                if (type.getSuperclass() != null) {
                  return (Codec<T>) lookup(type.getSuperclass());
                }
                return null;
              })
      ).orElseThrow(() -> new CodecException(
          String.format("No codec registered for %s or its super classes!",
              type.getName())));
    }
  }

  /**
   * Indicates if context has a registered {@link Codec} for the specified class.
   *
   * @param clazz An instance of {@link Class}.
   *
   * @return {@code true} if the supplied class has a registered codec, {@code false} otherwise.
   */
  public boolean hasRegisteredCodec(final Class<?> clazz) {
    Objects.requireNonNull(clazz);
    return codecs.containsKey(clazz);
  }

}
