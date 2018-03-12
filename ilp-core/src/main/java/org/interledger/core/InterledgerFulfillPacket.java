package org.interledger.core;

import org.interledger.annotations.Immutable;
import org.interledger.cryptoconditions.PreimageSha256Fulfillment;

import org.immutables.value.Value.Default;

import java.util.Arrays;

public interface InterledgerFulfillPacket extends InterledgerPacket {

  /**
   * Get the default builder.
   *
   * @return a {@link InterledgerFulfillPacketBuilder} instance.
   */
  static InterledgerFulfillPacketBuilder builder() {
    return new InterledgerFulfillPacketBuilder();
  }

  PreimageSha256Fulfillment getFulfillment();

  /**
   * Arbitrary data for the sender that is set by the transport layer of a payment (for example,
   * this may contain PSK data).
   *
   * @return A byte array.
   */
  byte[] getData();

  @Immutable
  abstract class AbstractInterledgerFulfillPacket implements InterledgerFulfillPacket {

    @Override
    @Default
    public byte[] getData() {
      return new byte[0];
    }
  }

}
