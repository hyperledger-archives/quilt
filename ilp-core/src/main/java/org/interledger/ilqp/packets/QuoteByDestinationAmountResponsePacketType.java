package org.interledger.ilqp.packets;

import org.interledger.InterledgerPacketType;

import java.net.URI;

/**
 * An implementation of {@link InterledgerPacketType} for ILQP quote responses.
 */
public class QuoteByDestinationAmountResponsePacketType extends
    InterledgerPacketType.AbstractInterledgerPacketType implements InterledgerPacketType {

  /**
   * No-args Constructor.
   */
  public QuoteByDestinationAmountResponsePacketType() {
    super(ILQP_QUOTE_BY_DESTINATION_AMOUNT_RESPONSE_TYPE,
        URI.create("https://interledger.org/ilqp/quote_by_destination_amount_response"));
  }

}
