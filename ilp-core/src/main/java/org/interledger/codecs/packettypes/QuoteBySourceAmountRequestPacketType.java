package org.interledger.codecs.packettypes;

import org.interledger.codecs.packettypes.InterledgerPacketType.AbstractInterledgerPacketType;

import java.net.URI;

/**
 * An implementation of {@link InterledgerPacketType} for ILQP quote requests.
 */
public class QuoteBySourceAmountRequestPacketType extends AbstractInterledgerPacketType implements
    InterledgerPacketType {

  /**
   * No-args Constructor.
   */
  public QuoteBySourceAmountRequestPacketType() {
    super(ILQP_QUOTE_BY_SOURCE_AMOUNT_REQUEST_TYPE,
        URI.create("https://interledger.org/ilqp/quote_by_source_amount_request"));
  }

}
