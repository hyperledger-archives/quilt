package org.interledger.codecs.oer.ilqp;

import org.interledger.InterledgerAddress;
import org.interledger.codecs.Codec;
import org.interledger.codecs.CodecContext;
import org.interledger.codecs.QuoteLiquidityRequestCodec;
import org.interledger.codecs.oer.OerUint32Codec.OerUint32;
import org.interledger.codecs.packettypes.InterledgerPacketType;
import org.interledger.ilqp.QuoteLiquidityRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * An implementation of {@link Codec} that reads and writes instances of
 * {@link QuoteLiquidityRequest}. in OER format.
 * 
 * @see "https://github.com/interledger/rfcs/blob/master/asn1/InterledgerQuotingProtocol.asn"
 */
public class QuoteLiquidityRequestOerCodec implements QuoteLiquidityRequestCodec {

  @Override
  public QuoteLiquidityRequest read(CodecContext context, InputStream inputStream)
      throws IOException {

    Objects.requireNonNull(context);
    Objects.requireNonNull(inputStream);

    /* read the Interledger Address. */
    final InterledgerAddress destinationAccount =
        context.read(InterledgerAddress.class, inputStream);

    /* read the destination hold duration which is a unit32 */
    long destinationHoldDuration = context.read(OerUint32.class, inputStream).getValue();

    return QuoteLiquidityRequest.Builder.builder()
        .destinationAccount(destinationAccount)
        .destinationHoldDuration(Duration.of(destinationHoldDuration, ChronoUnit.MILLIS))
        .build();
  }

  @Override
  public void write(CodecContext context, QuoteLiquidityRequest instance,
      OutputStream outputStream) throws IOException {
  
    Objects.requireNonNull(context);
    Objects.requireNonNull(instance);
    Objects.requireNonNull(outputStream);

    /* write the packet type. */
    context.write(InterledgerPacketType.class, this.getTypeId(), outputStream);

    /* destination account */
    context.write(InterledgerAddress.class, instance.getDestinationAccount(), outputStream);
    
    /* destination hold duration, in milliseconds */
    context.write(OerUint32.class, new OerUint32(instance.getDestinationHoldDuration().toMillis()),
        outputStream);
  }
}
