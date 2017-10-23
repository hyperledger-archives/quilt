package org.interledger.codecs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A generic coder/decoder interface for all Interledger objects.
 */
public interface Codec<T> {

  /**
   * Read an object from the buffer according to the rules defined in the {@link CodecContext}.
   *
   * @param context     An instance of {@link CodecContext}.
   * @param inputStream An instance of {@link InputStream} to read data from.
   *
   * @return An instance of {@link T} as decoded from {@code inputStream}.
   *
   * @throws IOException If anything goes wrong reading from the {@link InputStream}.
   */
  T read(CodecContext context, InputStream inputStream) throws IOException;

  /**
   * Write an object to the {@code outputStream} according to the rules defined in the
   * {@code context}.
   *
   * @param context      An instance of {@link CodecContext}.
   * @param instance     An instance of type {@link T}.
   * @param outputStream An instance of {@link OutputStream} to write data to.
   *
   * @throws IOException If anything goes wrong writing to the {@link OutputStream}
   */
  void write(CodecContext context, T instance, OutputStream outputStream) throws IOException;

  /**
   * Writes an {@link Object} to the {@code outputStream} by attempting to convert it to a proper
   * type.
   *
   * @param context      An instance of {@link CodecContext}.
   * @param instance     An instance of type {@link Object}.
   * @param outputStream An instance of {@link OutputStream} to write data to.
   *
   * @throws IOException If anything goes wrong writing to the {@link OutputStream}
   */
  @SuppressWarnings("unchecked")
  default void writeObject(final CodecContext context, final Object instance,
      final OutputStream outputStream) throws IOException {
    write(context, (T) instance, outputStream);
  }
}
