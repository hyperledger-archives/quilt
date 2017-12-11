package org.interledger.ilqp;

import org.interledger.InterledgerPacketType;
import org.interledger.codecs.InterledgerPacketCodec;
import org.interledger.ilqp.packets.QuoteLiquidityResponsePacketType;

import org.hyperledger.quilt.codecs.framework.Codec;

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
