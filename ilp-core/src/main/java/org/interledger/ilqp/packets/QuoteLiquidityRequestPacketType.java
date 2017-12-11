package org.interledger.ilqp.packets;

import org.interledger.InterledgerPacketType;

import java.net.URI;

/**
 * An implementation of {@link InterledgerPacketType} for ILQP Liquidity requests.
 */
public class QuoteLiquidityRequestPacketType
    extends InterledgerPacketType.AbstractInterledgerPacketType
    implements InterledgerPacketType {

  /**
   * No-args Constructor.
   */
  public QuoteLiquidityRequestPacketType() {
    super(ILQP_QUOTE_LIQUIDITY_REQUEST_TYPE,
        URI.create("https://interledger.org/ilqp/quote_liquidity_request"));
  }
}
