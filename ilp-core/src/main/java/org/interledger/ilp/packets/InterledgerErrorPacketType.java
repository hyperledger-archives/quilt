package org.interledger.ilp.packets;

import org.interledger.InterledgerPacketType;

import java.net.URI;

/**
 * An implementation of {@link InterledgerPacketType} for ILP Error packets.
 */
public class InterledgerErrorPacketType extends InterledgerPacketType.AbstractInterledgerPacketType
    implements InterledgerPacketType {

  /**
   * No-args Constructor.
   */
  public InterledgerErrorPacketType() {
    super(INTERLEDGER_PROTOCOL_ERROR, URI.create("https://interledger.org/protocol_error"));
  }
}
