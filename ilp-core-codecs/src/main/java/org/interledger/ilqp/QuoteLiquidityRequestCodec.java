package org.interledger.ilqp;

import org.interledger.InterledgerPacketType;
import org.interledger.codecs.InterledgerPacketCodec;
import org.interledger.ilqp.packets.QuoteLiquidityRequestPacketType;

import org.hyperledger.quilt.codecs.framework.Codec;

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
