package org.interledger.codecs.oer;

import org.interledger.InterledgerAddress;
import org.interledger.codecs.Codec;
import org.interledger.codecs.CodecContext;
import org.interledger.codecs.oer.OerSequenceOfAddressCodec.OerSequenceOfAddress;
import org.interledger.codecs.oer.OerUint8Codec.OerUint8;

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
public class OerSequenceOfAddressCodec implements Codec<OerSequenceOfAddress> {

  @Override
  public OerSequenceOfAddress read(final CodecContext context, final InputStream inputStream)
      throws IOException {
    Objects.requireNonNull(context);
    Objects.requireNonNull(inputStream);

    // Read the quantity to get the number of addresses...
    final int numAddresses = context.read(OerUint8.class, inputStream).getValue();

    final List<InterledgerAddress> addressList = IntStream.range(0, numAddresses)
        .mapToObj(index -> {
          try {
            return context.read(InterledgerAddress.class, inputStream);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        })
        .collect(Collectors.toList());

    return new OerSequenceOfAddress(addressList);
  }

  @Override
  public void write(CodecContext context, OerSequenceOfAddress instance, OutputStream outputStream)
      throws IOException {

    // Write the length...
    context.write(new OerUint8(instance.getInterledgerAddresses().size()), outputStream);

    // Write the addresses...
    instance.getInterledgerAddresses().forEach(address -> {
      try {
        context.write(address, outputStream);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * An typing mechanism for registering codecs.
   */
  public static class OerSequenceOfAddress {

    private List<InterledgerAddress> interledgerAddresses;

    public OerSequenceOfAddress(final List<InterledgerAddress> interledgerAddresses) {
      this.interledgerAddresses = interledgerAddresses;
    }

    public List getInterledgerAddresses() {
      return interledgerAddresses;
    }

    @Override
    public boolean equals(Object object) {
      if (this == object) {
        return true;
      }
      if (object == null || getClass() != object.getClass()) {
        return false;
      }

      OerSequenceOfAddress that = (OerSequenceOfAddress) object;

      return interledgerAddresses.equals(that.interledgerAddresses);
    }

    @Override
    public int hashCode() {
      return interledgerAddresses.hashCode();
    }

    @Override
    public String toString() {
      return "OerSequenceOf{"
          + "interledgerAddresses=" + interledgerAddresses
          + '}';
    }
  }

}
