package org.interledger.codecs.oer.ilqp;

import org.interledger.InterledgerAddress;
import org.interledger.codecs.Codec;
import org.interledger.codecs.CodecContext;
import org.interledger.codecs.QuoteByDestinationAmountRequestCodec;
import org.interledger.codecs.oer.OerUint32Codec.OerUint32;
import org.interledger.codecs.oer.OerUint64Codec.OerUint64;
import org.interledger.codecs.packettypes.InterledgerPacketType;
import org.interledger.ilqp.QuoteByDestinationAmountRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * An implementation of {@link Codec} that reads and writes instances of {@link
 * QuoteByDestinationAmountRequest}. in OER format.
 *
 * @see "https://github.com/interledger/rfcs/blob/master/asn1/InterledgerQuotingProtocol.asn"
 */
public class QuoteByDestinationAmountRequestOerCodec
    implements QuoteByDestinationAmountRequestCodec {

  @Override
  public QuoteByDestinationAmountRequest read(CodecContext context, InputStream inputStream)
      throws IOException {

    Objects.requireNonNull(context);
    Objects.requireNonNull(inputStream);

    /* read the Interledger Address. */
    final InterledgerAddress destinationAccount =
        context.read(InterledgerAddress.class, inputStream);

    /* read the destination amount, which is a uint64 */
    final BigInteger destinationAmount = context.read(OerUint64.class, inputStream).getValue();

    /* read the destination hold duration which is a unit32 */
    final long destinationHoldDuration = context.read(OerUint32.class, inputStream).getValue();

    return QuoteByDestinationAmountRequest.Builder.builder().destinationAccount(destinationAccount)
        .destinationAmount(destinationAmount)
        .destinationHoldDuration(Duration.of(destinationHoldDuration, ChronoUnit.MILLIS)).build();
  }

  @Override
  public void write(CodecContext context, QuoteByDestinationAmountRequest instance,
      OutputStream outputStream) throws IOException {

    Objects.requireNonNull(context);
    Objects.requireNonNull(instance);
    Objects.requireNonNull(outputStream);

    /* write the packet type. */
    context.write(InterledgerPacketType.class, this.getTypeId(), outputStream);

    /* destination account */
    context.write(InterledgerAddress.class, instance.getDestinationAccount(), outputStream);

    /* destination amount */
    context.write(OerUint64.class, new OerUint64(instance.getDestinationAmount()), outputStream);

    /* destination hold duration, in milliseconds */
    context.write(OerUint32.class, new OerUint32(instance.getDestinationHoldDuration().toMillis()),
        outputStream);
  }
}
