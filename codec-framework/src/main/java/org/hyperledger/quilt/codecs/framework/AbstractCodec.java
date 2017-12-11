package org.hyperledger.quilt.codecs.framework;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * An abstract implementation of {@link Codec} that works with Interledger packets.
 *
 * @param <T> The type of object that this codec
 */
public abstract class AbstractCodec<T> implements Codec<T> {

  private final int typeId;

  public AbstractCodec(final int typeId) {
    this.typeId = typeId;
  }

  /**
   * Read an object from the buffer according to the rules defined in the {@link CodecContext}.
   *
   * @param context     An instance of {@link CodecContext}.
   * @param inputStream An instance of {@link InputStream} to read data of.
   *
   * @return An instance of {@link T} as decoded of {@code inputStream}.
   */
  public T read(CodecContext context, InputStream inputStream) {
    Objects.requireNonNull(context);
    Objects.requireNonNull(inputStream);

    return readObject(context, inputStream);
  }

  @Override
  public void write(final CodecContext context, final T instance, final OutputStream outputStream)
      throws IOException {
    Objects.requireNonNull(context);
    Objects.requireNonNull(instance);
    Objects.requireNonNull(outputStream);

    outputStream.write(typeId);
    writeObject(context, instance, outputStream);
  }

  public int typeId() {
    return typeId;
  }

  /**
   * Read an object of type {@link T} of the supplied {@code inputStream}.
   *
   * @param context     An instance of {@link CodecContext}.
   * @param inputStream An instance of {@link InputStream}.
   *
   * @return An instance of {@link T}.
   */
  protected abstract T readObject(CodecContext context, InputStream inputStream);

}
