package org.interledger.codecs.framework;

import org.interledger.codecs.asn.AsnObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A generic serializer/deserializer interface for all ASN.1 objects.
 */
public interface AsnObjectSerializer<T extends AsnObject> {

  /**
   * Read an ASN.1 object from the buffer according to the rules defined in the
   * {@link SerializationContext}.
   *
   * @param context     An instance of {@link SerializationContext}.
   * @param instance    An instance of {@link AsnObject}.
   * @param inputStream An instance of {@link InputStream} to read data from.
   * @throws IOException If anything goes wrong reading from the {@link InputStream}.
   */
  void read(SerializationContext context, T instance, InputStream inputStream) throws IOException;

  /**
   * Write an object to the {@code outputStream} according to the rules defined in the
   * {@code context}.
   *
   * @param context      An instance of {@link SerializationContext}.
   * @param instance     An instance of {@link AsnObject}.
   * @param outputStream An instance of {@link OutputStream} to write data to.
   * @throws IOException If anything goes wrong writing to the {@link OutputStream}
   */
  void write(SerializationContext context, T instance, OutputStream outputStream) throws
      IOException;

}
