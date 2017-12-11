package org.interledger.ilp.packets;

import org.interledger.InterledgerPacketType;
import org.interledger.InterledgerPacketType.AbstractInterledgerPacketType;

import java.net.URI;

/**
 * An implementation of {@link InterledgerPacketType} for ILP Payment packets.
 */
public class PaymentPacketType extends AbstractInterledgerPacketType
    implements InterledgerPacketType {

  /**
   * No-args Constructor.
   */
  public PaymentPacketType() {
    super(ILP_PAYMENT_TYPE, URI.create("https://interledger.org/payment_packet"));
  }
}
