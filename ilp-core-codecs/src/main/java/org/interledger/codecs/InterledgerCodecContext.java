package org.interledger.codecs;

import org.interledger.InterledgerPacket;
import org.interledger.InterledgerPacketType;

import org.hyperledger.quilt.codecs.framework.Codec;
import org.hyperledger.quilt.codecs.framework.CodecContext;
import org.hyperledger.quilt.codecs.framework.CodecException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A contextual object for matching instances of {@link Codec} to specific class types.
 */
public class InterledgerCodecContext extends CodecContext {

  /**
   * A map of codec that can encode/decode based on a typeId prefix. This is used when an
   * undetermined packet of bytes are coming in off the wire, and we need to determine how to decode
   * these bytes based on the first typeId header.
   */
  private final Map<InterledgerPacketType, Class<?>> packetTypes;

  /**
   * A map of codec that can encode/decode based on a typeId prefix. This is used when an
   * undetermined packet of bytes are coming in off the wire, and we need to determine how to decode
   * these bytes based on the first typeId header.
   */
  private final Map<InterledgerPacketType, InterledgerPacketCodec<?>> packetCodecs;

  /**
   * No-args Constructor.
   */
  public InterledgerCodecContext() {
    this.packetTypes = new ConcurrentHashMap<>();
    this.packetCodecs = new ConcurrentHashMap<>();
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
  @Override
  public <T> InterledgerCodecContext register(
      final Class<? extends T> type, final Codec<T> converter) {
    Objects.requireNonNull(type);
    Objects.requireNonNull(converter);

    super.register(type, converter);

    if (converter instanceof InterledgerPacketCodec<?>) {
      InterledgerPacketCodec<?> commandTypeConverter = (InterledgerPacketCodec) converter;
      this.packetTypes.put(commandTypeConverter.getTypeId(), type);
      this.packetCodecs.put(commandTypeConverter.getTypeId(), commandTypeConverter);
    }
    return this;
  }

  /**
   * Read an {@link InterledgerPacket} from the {@code inputStream}.
   *
   * @param typeId      An instance of {@link InterledgerPacketType}.
   * @param inputStream An instance of {@link InputStream}
   *
   * @return An instance of {@link InterledgerPacket} as read from the input stream.
   *
   * @throws IOException If anything goes wrong reading from the {@code inputStream}.
   */
  public InterledgerPacket read(final InterledgerPacketType typeId, final InputStream inputStream)
      throws IOException {
    Objects.requireNonNull(inputStream);
    return (InterledgerPacket) lookup(typeId).read(this, inputStream);
  }

  /**
   * Helper method that accepts an {@link InputStream}, detects the type of the packet to be read
   * and decodes the packet to {@link InterledgerPacket}. Because {@link InterledgerPacket} is
   * simply a marker interface, callers might prefer to utilize the functionality supplied by {@link
   * #readAndHandle(InputStream, InterledgerPacket.Handler)} or
   * {@link #readAndHandle(InputStream, InterledgerPacket.VoidHandler)}.
   *
   * @param inputStream An instance of {@link InputStream} that contains bytes in a certain
   *                    encoding.
   *
   * @return An instance of {@link InterledgerPacket}.
   *
   * @throws IOException If anything goes wrong reading from the {@code inputStream}.
   */
  public InterledgerPacket read(final InputStream inputStream) throws IOException {
    Objects.requireNonNull(inputStream);

    final InterledgerPacketType type = InterledgerPacketType.fromTypeId(inputStream.read());
    return read(type, inputStream);
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
  @Override
  public <T> T read(final Class<T> type, final InputStream inputStream) throws IOException {
    Objects.requireNonNull(type);
    Objects.requireNonNull(inputStream);

    if (InterledgerPacket.class.isAssignableFrom(type)) {
      //noinspection ResultOfMethodCallIgnored
      inputStream.read(); // swallow type field
    }
    return super.read(type, inputStream);
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
  @Override
  public <T> T read(final Class<T> type, final byte[] data) {
    Objects.requireNonNull(type);
    Objects.requireNonNull(data);

    try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
      if (InterledgerPacket.class.isAssignableFrom(type)) {
        //noinspection ResultOfMethodCallIgnored
        bais.read(); // swallow type field
      }
      return read(type, bais);
    } catch (IOException e) {
      throw new CodecException("Unable to decode " + type.getCanonicalName(), e);
    }

  }

  /**
   * Read an object from the buffer according to the rules defined in the {@link CodecContext}, and
   * handle any terminating logic inside of {@code packetHandler}.
   *
   * @param inputStream   An instance of {@link InputStream} to read data from.
   * @param packetHandler A {@link InterledgerPacket.VoidHandler} that allows callers to supply
   *                      business logic to be applied against the packet, depending on what the
   *                      runtime-version of the packet ultimately is.
   *
   * @throws IOException If anything goes wrong while reading from the InputStream.
   */
  public void readAndHandle(final InputStream inputStream,
                            final InterledgerPacket.VoidHandler packetHandler) throws IOException {

    Objects.requireNonNull(inputStream);
    Objects.requireNonNull(packetHandler);

    final InterledgerPacket interledgerPacket = this.read(inputStream);
    packetHandler.execute(interledgerPacket);
  }

  /**
   * Read an object from {@code inputStream} according to the rules defined in the {@code context},
   * handle any concrete logic inside of {@code packetHandler}, and return a result.
   *
   * @param <R>           This describes the type parameter of the object to be read.
   * @param inputStream   An instance of {@link InputStream} to read data from.
   * @param packetHandler A {@link InterledgerPacket.Handler} that allows callers to supply business
   *                      logic to be applied against the packet, depending on what the
   *                      runtime-version of the packet ultimately is, and then return a value.
   *
   * @return An instance of {@link R}.
   *
   * @throws IOException If anything goes wrong while reading from the InputStream.
   */
  public <R> R readAndHandle(final InputStream inputStream,
                             final InterledgerPacket.Handler<R> packetHandler) throws IOException {
    Objects.requireNonNull(inputStream);
    Objects.requireNonNull(packetHandler);

    final InterledgerPacket interledgerPacket = this.read(inputStream);
    return packetHandler.execute(interledgerPacket);
  }


  /**
   * Lookup a specific {@link Codec} based upon the supplied {@code typeId}.
   *
   * @param typeId An instance of {@link InterledgerPacketType}.
   */
  private Codec<?> lookup(final InterledgerPacketType typeId) {
    if (packetCodecs.containsKey(typeId)) {
      return packetCodecs.get(typeId);
    }
    throw new CodecException(
        "No " + InterledgerPacketCodec.class.getName() + " registered for typeId " + typeId);
  }

}
