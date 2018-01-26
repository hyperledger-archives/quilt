package org.interledger.codecs.oer.ilp;

import org.interledger.codecs.Codec;
import org.interledger.codecs.CodecContext;
import org.interledger.codecs.ConditionCodec;
import org.interledger.codecs.oer.OerUint256Codec.OerUint256;
import org.interledger.cryptoconditions.Condition;
import org.interledger.cryptoconditions.PreimageSha256Condition;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * An implementation of {@link Codec} that reads and writes instances of {@link Condition}.
 *
 * <p>In universal mode ILP, conditions are assumed to always be PreimageSha256 Crypto Conditions
 * encoded simply as a 32 byte hash.
 *
 * <p>The preimage of the hash is always 32 bytes.
 *
 */
public class ConditionOerCodec implements ConditionCodec {

  @Override
  public Condition read(final CodecContext context, final InputStream inputStream)
      throws IOException {
    Objects.requireNonNull(context);
    Objects.requireNonNull(inputStream);
    final byte[] value = context.read(OerUint256.class, inputStream)
        .getValue();

    //Cost (equal to the length of the preimage) is always 32 bytes in universal mode ILP
    return PreimageSha256Condition.fromCostAndFingerprint(32, value);
  }

  @Override
  public void write(final CodecContext context, final Condition instance,
      final OutputStream outputStream) throws IOException {
    Objects.requireNonNull(context);
    Objects.requireNonNull(instance);
    Objects.requireNonNull(outputStream);

    if (instance.getCost() != 32) {
      throw new IllegalArgumentException("Instance.getCost() must be equal to 32");
    }

    context.write(OerUint256.class, new OerUint256(instance.getFingerprint()), outputStream);
  }

}
