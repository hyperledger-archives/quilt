package org.interledger.ilqp.packets;

import org.interledger.InterledgerPacketType;

import java.net.URI;

/**
 * An implementation of {@link InterledgerPacketType} for ILQP quote responses.
 */
public class QuoteBySourceAmountResponsePacketType
    extends InterledgerPacketType.AbstractInterledgerPacketType implements
    InterledgerPacketType {

  /**
   * No-args Constructor.
   */
  public QuoteBySourceAmountResponsePacketType() {
    super(ILQP_QUOTE_BY_SOURCE_AMOUNT_RESPONSE_TYPE,
        URI.create("https://interledger.org/ilqp/quote_by_source_amount_response"));
  }

}
