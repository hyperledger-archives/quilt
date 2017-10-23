package org.interledger.codecs.packettypes;

import org.interledger.codecs.packettypes.InterledgerPacketType.AbstractInterledgerPacketType;

import java.net.URI;

/**
 * An implementation of {@link InterledgerPacketType} for ILQP Liquidity responses.
 */
public class QuoteLiquidityResponsePacketType extends AbstractInterledgerPacketType
    implements InterledgerPacketType {

  /**
   * No-args Constructor.
   */
  public QuoteLiquidityResponsePacketType() {
    super(ILQP_QUOTE_LIQUIDITY_RESPONSE_TYPE,
        URI.create("https://interledger.org/ilqp/quote_liquidity_response"));
  }

}
