package org.interledger.codecs.oer.ipr;

import org.interledger.codecs.Codec;
import org.interledger.codecs.CodecContext;
import org.interledger.codecs.InterledgerPaymentRequestCodec;
import org.interledger.codecs.oer.OerUint8Codec.OerUint8;
import org.interledger.cryptoconditions.Condition;
import org.interledger.cryptoconditions.PreimageSha256Condition;
import org.interledger.ilp.InterledgerPayment;
import org.interledger.ipr.InterledgerPaymentRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * An implementation of {@link Codec} that reads and writes instances of
 * {@link InterledgerPaymentRequest}.
 */
public class InterledgerPaymentRequestOerCodec implements InterledgerPaymentRequestCodec {

  @Override
  public InterledgerPaymentRequest read(final CodecContext context, final InputStream inputStream)
      throws IOException {
    Objects.requireNonNull(context);
    Objects.requireNonNull(inputStream);

    final int version = context.read(OerUint8.class, inputStream)
        .getValue();

    if (version != 2) {
      throw new RuntimeException("Unknown IPR version: " + version);
    }

    final InterledgerPayment packet = context.read(InterledgerPayment.class, inputStream);
    final PreimageSha256Condition condition
        = context.read(PreimageSha256Condition.class, inputStream);

    return InterledgerPaymentRequest.builder()
        .payment(packet)
        .condition(condition)
        .build();
  }

  @Override
  public void write(final CodecContext context, final InterledgerPaymentRequest instance,
      final OutputStream outputStream) throws IOException {
    Objects.requireNonNull(context);
    Objects.requireNonNull(instance);
    Objects.requireNonNull(outputStream);

    context.write(OerUint8.class, new OerUint8(instance.getVersion()), outputStream);
    context.write(InterledgerPayment.class, instance.getInterledgerPayment(), outputStream);
    context.write(Condition.class, instance.getCondition(), outputStream);
  }

}
