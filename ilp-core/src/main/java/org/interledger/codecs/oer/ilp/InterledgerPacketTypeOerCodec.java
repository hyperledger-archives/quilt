package org.interledger.codecs.oer.ilp;

import org.interledger.InterledgerAddress;
import org.interledger.codecs.Codec;
import org.interledger.codecs.CodecContext;
import org.interledger.codecs.CodecException;
import org.interledger.codecs.InterledgerPacketTypeCodec;
import org.interledger.codecs.oer.OerUint8Codec.OerUint8;
import org.interledger.codecs.packettypes.InterledgerPacketType;
import org.interledger.codecs.packettypes.InterledgerPacketType.InvalidPacketTypeException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * An implementation of {@link Codec} that reads and writes instances of {@link InterledgerAddress}.
 */
public class InterledgerPacketTypeOerCodec implements InterledgerPacketTypeCodec {

  @Override
  public InterledgerPacketType read(final CodecContext context, final InputStream inputStream)
      throws IOException {
    Objects.requireNonNull(context);
    Objects.requireNonNull(inputStream);

    final int typeId = context.read(OerUint8.class, inputStream)
        .getValue();

    try {
      return InterledgerPacketType.fromTypeId(typeId);
    } catch (InvalidPacketTypeException e) {
      throw new CodecException("Encountered unsupported Interledger Packet Type.  Please extend "
          + "InterledgerPacketTypeCodec and register it with the CodecContext to support this"
          + "new type.", e);
    }
  }

  @Override
  public void write(final CodecContext context, final InterledgerPacketType instance,
      final OutputStream outputStream) throws IOException {
    Objects.requireNonNull(context);
    Objects.requireNonNull(instance);
    Objects.requireNonNull(outputStream);

    context.write(OerUint8.class, new OerUint8(instance.getTypeIdentifier()), outputStream);
  }
}
