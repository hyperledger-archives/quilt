package org.hyperledger.quilt.codecs.oer;

import org.hyperledger.quilt.codecs.framework.Codec;
import org.hyperledger.quilt.codecs.framework.CodecContext;
import org.hyperledger.quilt.codecs.oer.OerUint8Codec.OerUint8;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * <p>A sequence-of type is encoded as a quantity field followed by the encoding of each occurrence
 * of the repeating field of the sequence-of type (zero or more). Extensible and non-extensible
 * sequence-of types are encoded in the same way. The quantity field is set to the number of
 * occurrences (not to the number of octets), and is encoded as an integer type with a lower bound
 * of zero and no upper bound.</p>
 */
public abstract class OerSequenceOfCodec<T> implements Codec<List<T>> {

  private final Codec<T> elementCodec;

  public OerSequenceOfCodec(Codec<T> elementCodec) {
    this.elementCodec = elementCodec;
  }

  @Override
  public List<T> read(final CodecContext context, final InputStream inputStream)
      throws IOException {
    Objects.requireNonNull(context);
    Objects.requireNonNull(inputStream);

    // Read the quantity to get the number of addresses...
    final int numAddresses = context.read(OerUint8.class, inputStream).getValue();

    final List<T> elements = IntStream.range(0, numAddresses)
        .mapToObj(index -> {
          try {
            return elementCodec.read(context, inputStream);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        })
        .collect(Collectors.toList());

    return elements;
  }

  @Override
  public void write(CodecContext context, List<T> instance, OutputStream outputStream)
      throws IOException {

    // Write the length...
    context.write(new OerUint8(instance.size()), outputStream);

    // Write the addresses...
    instance.forEach(element -> {
      try {
        elementCodec.write(context, element, outputStream);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

}
