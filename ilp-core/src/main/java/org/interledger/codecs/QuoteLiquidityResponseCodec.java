package org.interledger.codecs;

import org.interledger.codecs.packettypes.InterledgerPacketType;
import org.interledger.codecs.packettypes.QuoteLiquidityResponsePacketType;
import org.interledger.ilqp.QuoteLiquidityResponse;

/**
 * An implementation of {@link Codec} that reads and writes instances of
 * {@link QuoteLiquidityResponse}.
 */
public interface QuoteLiquidityResponseCodec
    extends InterledgerPacketCodec<QuoteLiquidityResponse> {

  InterledgerPacketType TYPE = new QuoteLiquidityResponsePacketType();

  @Override
  default InterledgerPacketType getTypeId() {
    return TYPE;
  }

}
