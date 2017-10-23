package org.interledger.codecs.oer.ilp;

import org.interledger.InterledgerAddress;
import org.interledger.codecs.Codec;
import org.interledger.codecs.CodecContext;
import org.interledger.codecs.InterledgerProtocolErrorCodec;
import org.interledger.codecs.oer.OerGeneralizedTimeCodec.OerGeneralizedTime;
import org.interledger.codecs.oer.OerIA5StringCodec.OerIA5String;
import org.interledger.codecs.oer.OerOctetStringCodec.OerOctetString;
import org.interledger.codecs.oer.OerSequenceOfAddressCodec.OerSequenceOfAddress;
import org.interledger.codecs.packettypes.InterledgerPacketType;
import org.interledger.ilp.InterledgerProtocolError;
import org.interledger.ilp.InterledgerProtocolError.ErrorCode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * <p>An implementation of {@link Codec} that reads and writes instances of {@link
 * InterledgerProtocolError}.</p>
 *
 * <p>The ASN.1 OER definition of an InterledgerProtocolError defines it as an extensible sequence.
 * Thus, this sequence must have the following: </p>
 *
 * <p><b>Presence Bitmap</b> The presence bitmap is encoded as a "bit string" with a fixed size
 * constraint, and has one bit for each field of the sequence type that has the keyword OPTIONAL or
 * DEFAULT, in specification order. Because the InterledgerProtocolError has no optional/default
 * values, there is no presence bitmap. As an example of this, reference "Overview of OER Encoding",
 * example B, which also has no presence bitmap since no fields are optional/default.</p>
 *
 * <p><b>Extension Presence Bitmap</b> This implementation does not currently support extensions,
 * and therefore does not encode nor decode an "extension presence bitmap". If it did, in order to
 * indicate an extension, the presence bitmap must be present, and the MSB of the bitmap must be 1,
 * and further rules. Reference section 2.8 "Encoding of a Sequence Type" in "Overview of OER" for
 * more details. </p>
 *
 * <p><b>Components</b> The rest of the packet is the concatenation of the encodings of the fields
 * of the sequence type that are present in the value, in specification order. </p>
 *
 * @see "http://www.oss.com/asn1/resources/books-whitepapers-pubs/Overview%20of%20OER.pdf"
 */
public class InterledgerProtocolProtocolErrorOerCodec implements InterledgerProtocolErrorCodec {

  @Override
  public InterledgerProtocolError read(final CodecContext context, final InputStream inputStream)
      throws IOException {
    Objects.requireNonNull(context);
    Objects.requireNonNull(inputStream);

    // 1. InterledgerProtocolError has no "presence bitmap". See javadoc for this class for more
    // details.

    // 2. InterledgerProtocolError has no "extension presence bitmap". See javadoc for this class
    // for more details.

    // 3. Read the code, which is an IA5String.
    final String code = context.read(OerIA5String.class, inputStream).getValue();

    // 4. Read the name, which is a IA5String.
    final String name = context.read(OerIA5String.class, inputStream).getValue();

    final ErrorCode errorCode = ErrorCode.of(code, name);

    // 5. Read the triggeredBy address, which is an InterledgerAddress
    final InterledgerAddress triggeredByAddress =
        context.read(InterledgerAddress.class, inputStream);

    // 6. Read the forwardedBy,which is a SEQUENCE OF InterledgerAddress
    final List<InterledgerAddress> addressList =
        context.read(OerSequenceOfAddress.class, inputStream).getInterledgerAddresses();

    // 7. Read the triggeredAt, which is a Timestamp
    final Instant triggeredAt = context.read(OerGeneralizedTime.class, inputStream).getValue();

    // 8. Read the data, which is an OctetString.
    final byte[] data = context.read(OerOctetString.class, inputStream)
        .getValue();

    return InterledgerProtocolError.builder()
        .errorCode(errorCode)
        .triggeredByAddress(triggeredByAddress)
        .forwardedByAddresses(addressList)
        .triggeredAt(triggeredAt)
        .data(data)
        .build();
  }

  @Override
  public void write(final CodecContext context, final InterledgerProtocolError instance,
      final OutputStream outputStream) throws IOException {
    Objects.requireNonNull(context);
    Objects.requireNonNull(instance);
    Objects.requireNonNull(outputStream);

    // 1. InterledgerProtocolError has no "presence bitmap". See javadoc for this class for more
    // details.

    // 2. InterledgerProtocolError has no "extension presence bitmap". See javadoc for this class
    // for more details.

    // 3. Write the packet type.
    context.write(InterledgerPacketType.class, this.getTypeId(), outputStream);

    // 4. Write the Error code, which is an IA5String.
    context.write(new OerIA5String(instance.getErrorCode().getCode()), outputStream);

    // 5. Write the Error name, which is a IA5String.
    context.write(new OerIA5String(instance.getErrorCode().getName()), outputStream);

    // 5. Write the triggeredBy address, which is an InterledgerAddress
    context.write(instance.getTriggeredByAddress(), outputStream);

    // 6. Write the forwardedBy addresses, which is a SEQUENCE OF InterledgerAddress
    context.write(new OerSequenceOfAddress(instance.getForwardedByAddresses()), outputStream);

    // 7. Write the triggeredAt, which is a Timestamp
    context.write(new OerGeneralizedTime(instance.getTriggeredAt()), outputStream);

    // 8. Write the data, which is an OctetString.
    instance.getData().ifPresent(data -> {
      try {
        context.write(OerOctetString.class, new OerOctetString(data), outputStream);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }
}
