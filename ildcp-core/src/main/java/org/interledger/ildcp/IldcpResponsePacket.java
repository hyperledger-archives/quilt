package org.interledger.ildcp;

import org.interledger.annotations.Immutable;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;

import org.immutables.value.Value.Derived;

import java.util.Objects;

/**
 * An extension of {@link InterledgerFulfillPacket} that can be used as an IL-DCP response over Interledger.
 */
public interface IldcpResponsePacket extends InterledgerFulfillPacket {

  byte[] EMPTY_DATA = new byte[32];
  InterledgerFulfillment EXECUTION_FULFILLMENT = InterledgerFulfillment.of(EMPTY_DATA);

  static IldcpResponsePacketBuilder builder() {
    return new IldcpResponsePacketBuilder();
  }

  /**
   * The fulfillment of an ILP packet for IL-DCP is always a 32-byte octet string all filled with zeros.
   */
  default InterledgerFulfillment getFulfillment() {
    return EXECUTION_FULFILLMENT;
  }

  /**
   * Exists to satisfy Immutables.
   */
  @Immutable
  abstract class AbstractIldcpResponsePacket implements IldcpResponsePacket {

    /**
     * The fulfillment of an ILP packet for IL-DCP is always a 32-byte octet string all filled with zeros.
     */
    @Derived
    @Override
    public InterledgerFulfillment getFulfillment() {
      return EXECUTION_FULFILLMENT;
    }

  }
}