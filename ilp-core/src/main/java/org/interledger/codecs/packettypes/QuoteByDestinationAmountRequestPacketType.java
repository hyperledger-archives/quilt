package org.interledger.codecs.packettypes;

import org.interledger.codecs.packettypes.InterledgerPacketType.AbstractInterledgerPacketType;

import java.net.URI;

/**
 * An implementation of {@link InterledgerPacketType} for ILQP Liquidity responses.
 */
public class QuoteByDestinationAmountRequestPacketType extends
    AbstractInterledgerPacketType implements InterledgerPacketType {

  /**
   * No-args Constructor.
   */
  public QuoteByDestinationAmountRequestPacketType() {
    super(ILQP_QUOTE_BY_DESTINATION_AMOUNT_REQUEST_TYPE,
        URI.create("https://interledger.org/ilqp/quote_by_destination_amount_request"));
  }

}
