package org.interledger.ilp.packets;

import org.interledger.InterledgerPacketType;
import org.interledger.InterledgerPacketType.AbstractInterledgerPacketType;

import java.net.URI;

/**
 * An implementation of {@link ForwardedPaymentPacketType} for ILP Payment packets.
 */
public class ForwardedPaymentPacketType extends AbstractInterledgerPacketType
    implements InterledgerPacketType {
  /**
   * No-args Constructor.
   */
  public ForwardedPaymentPacketType() {
    super(ILP_PAYMENT_TYPE, URI.create("https://interledger.org/forwardedPayment_packet"));
  }
}

