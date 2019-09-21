package org.interledger.ildcp;

import org.interledger.core.Immutable;
import org.interledger.core.InterledgerResponsePacket;

@Immutable
public interface InterledgerShampooPacket extends InterledgerResponsePacket {

  @Override
  default byte[] getData() {
    return new byte[0];
  }

  static InterledgerShampooPacketBuilder builder() {
    return new InterledgerShampooPacketBuilder();
  }

}