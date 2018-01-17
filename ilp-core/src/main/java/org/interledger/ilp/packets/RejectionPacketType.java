package org.interledger.ilp.packets;

import org.interledger.InterledgerPacketType;
import org.interledger.InterledgerPacketType.AbstractInterledgerPacketType;

import java.net.URI;

/**
 * An implementation of {@link RejectionPacketType} for ILP Payment packets.
 */
public class RejectionPacketType extends AbstractInterledgerPacketType
    implements InterledgerPacketType {

  /**
   * No-args Constructor.
   */
  public RejectionPacketType() {
    super(ILP_PAYMENT_TYPE, URI.create("https://interledger.org/rejection_packet"));
  }
}
