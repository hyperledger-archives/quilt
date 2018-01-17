package org.interledger.ilp.packets;

import org.interledger.InterledgerPacketType;
import org.interledger.InterledgerPacketType.AbstractInterledgerPacketType;

import java.net.URI;

/**
 * An implementation of {@link FulfillmentPacketType} for ILP Payment packets.
 */
public class FulfillmentPacketType extends AbstractInterledgerPacketType
    implements InterledgerPacketType {  /**
   * No-args Constructor.
   */
  public FulfillmentPacketType() {
    super(ILP_PAYMENT_TYPE, URI.create("https://interledger.org/fulfillment_packet"));
  }
}
