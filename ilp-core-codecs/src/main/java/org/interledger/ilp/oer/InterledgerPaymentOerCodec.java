package org.interledger.ilp.oer;

import org.interledger.InterledgerAddress;
import org.interledger.InterledgerPacketType;
import org.interledger.ilp.InterledgerPayment;
import org.interledger.ilp.InterledgerPaymentCodec;

import org.hyperledger.quilt.codecs.framework.Codec;
import org.hyperledger.quilt.codecs.framework.CodecContext;
import org.hyperledger.quilt.codecs.oer.OerOctetStringCodec.OerOctetString;
import org.hyperledger.quilt.codecs.oer.OerUint64Codec.OerUint64;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Objects;

/**
 * <p>An implementation of {@link Codec} that reads and writes instances of {@link
 * InterledgerPayment}.</p>
 *
 * <p>The ASN.1 OER definition of an InterledgerPayment defines it as an extensible sequence. Thus,
 * this sequence must have the following: </p>
 *
 * <p><b>Presence Bitmap</b> The presence bitmap is encoded as a "bit string" with a fixed size
 * constraint, and has one bit for each field of the sequence type that has the keyword OPTIONAL or
 * DEFAULT, in specification order. Because the InterledgerPayment has no optional/default values,
 * there is no presence bitmap. As an example of this, reference "Overview of OER Encoding" ()
 * example B, which also has no presence bitmap since no fields are optional/default.</p>
 *
 * <p><b>Extension Presence Bitmap</b> This implementation does not currently support extensions,
 * and therefore does not encode or decode an "extension presence bitmap". If it did, in order to
 * indicate an extension, the presence bitmap must be present, and the MSB of the bitmap must be 1,
 * and further rules. Reference section 2.8 "Encoding of a Sequence Type" in "Overview of OER" for
 * more details. </p>
 *
 * <p><b>Components</b> The rest of the packet is the concatenation of the encodings of the fields
 * of the sequence type that are present in the value, in specification order. </p>
 *
 * @see "http://www.oss.com/asn1/resources/books-whitepapers-pubs/Overview%20of%20OER.pdf"
 */
public class InterledgerPaymentOerCodec implements InterledgerPaymentCodec {

  @Override
  public InterledgerPayment read(final CodecContext context, final InputStream inputStream)
      throws IOException {
    Objects.requireNonNull(context);
    Objects.requireNonNull(inputStream);

    // 1. InterledgerPayment has no "presence bitmap". See javadoc for this class for more details.

    // 2. InterledgerPayment has no "extension presence bitmap". See javadoc for this class for
    // more details.

    // 3. Read the destinationAmount, which is a UInt64.
    final BigInteger destinationAmount = context.read(OerUint64.class, inputStream).getValue();

    // 4. Read the Interledger Address.
    final InterledgerAddress destinationAccount =
        context.read(InterledgerAddress.class, inputStream);

    // 5. Read the data portion of the packet.
    final byte[] data = context.read(OerOctetString.class, inputStream)
        .getValue();

    return InterledgerPayment.builder()
        .destinationAmount(destinationAmount)
        .destinationAccount(destinationAccount)
        .data(data)
        .build();
  }

  @Override
  public void write(final CodecContext context, final InterledgerPayment instance,
      final OutputStream outputStream) throws IOException {
    Objects.requireNonNull(context);
    Objects.requireNonNull(instance);
    Objects.requireNonNull(outputStream);

    // 1. InterledgerPayment has no "presence bitmap". See javadoc for this class for more details.

    // 2. InterledgerPayment has no "extension presence bitmap". See javadoc for this class for
    // more details.

    // 3. Write the packet type.
    context.write(InterledgerPacketType.class, this.getTypeId(), outputStream);

    // 4. Write the amount, which is a UInt64 (fixed at 8 octets)
    context
        .write(OerUint64.class, new OerUint64(instance.getDestinationAmount()), outputStream);

    // 5. Write the Interledger Address as an IA5String.
    context.write(InterledgerAddress.class, instance.getDestinationAccount(), outputStream);

    // 6. Write the data portion of the packet.
    context.write(OerOctetString.class, new OerOctetString(instance.getData()), outputStream);
  }


}
