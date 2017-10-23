package org.interledger.codecs;

import org.interledger.codecs.packettypes.InterledgerPacketType;
import org.interledger.codecs.packettypes.QuoteLiquidityRequestPacketType;
import org.interledger.ilqp.QuoteLiquidityRequest;

/**
 * An implementation of {@link Codec} that reads and writes instances of
 * {@link QuoteLiquidityRequest}.
 */
public interface QuoteLiquidityRequestCodec
    extends InterledgerPacketCodec<QuoteLiquidityRequest> {

  InterledgerPacketType TYPE = new QuoteLiquidityRequestPacketType();

  @Override
  default InterledgerPacketType getTypeId() {
    return TYPE;
  }
}
