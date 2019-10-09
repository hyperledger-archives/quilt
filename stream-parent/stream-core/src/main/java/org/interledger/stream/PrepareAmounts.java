package org.interledger.stream;

import org.interledger.core.Immutable;

import com.google.common.primitives.UnsignedLong;

@Immutable
public interface PrepareAmounts {

  UnsignedLong getAmountToSend();

  UnsignedLong getMinimumAmountToAccept();

  static PrepareAmountsBuilder of() {
    return new PrepareAmountsBuilder();
  }

}
