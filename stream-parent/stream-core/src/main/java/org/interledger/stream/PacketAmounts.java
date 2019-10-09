package org.interledger.stream;

import org.interledger.core.Immutable;

import com.google.common.primitives.UnsignedLong;

@Immutable
public interface PacketAmounts {

  UnsignedLong getAmountToSend();

  UnsignedLong getMinimumAmountToAccept();

  static PacketAmountsBuilder of() {
    return new PacketAmountsBuilder();
  }

}
