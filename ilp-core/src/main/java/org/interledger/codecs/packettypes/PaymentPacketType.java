package org.interledger.codecs.packettypes;

import org.interledger.codecs.packettypes.InterledgerPacketType.AbstractInterledgerPacketType;

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
