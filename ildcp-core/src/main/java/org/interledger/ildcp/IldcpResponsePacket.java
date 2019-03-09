package org.interledger.ildcp;

import org.interledger.annotations.Immutable;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;

import org.immutables.value.Value.Default;

/**
 * An extension of {@link InterledgerFulfillPacket} that is also a {@link IldcpResponsePacket} that can be used as an
 * IL-DCP response over Interledger.
 */
public interface IldcpResponsePacket extends InterledgerFulfillPacket {

  InterledgerFulfillment EXECUTION_FULFILLMENT = InterledgerFulfillment.of(new byte[32]);

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
   * The {@link IldcpResponse} encoded into the <tt>data</tt> field of this packet.
   *
   * @return The {@link IldcpResponse}.
   */
  IldcpResponse getIldcpResponse();

  /**
   * Exists to satisfy Immutables.
   */
  @Immutable
  abstract class AbstractIldcpResponsePacket implements IldcpResponsePacket {

    /**
     * The fulfillment of an ILP packet for IL-DCP is always a 32-byte octet string all filled with zeros.
     */
    @Default
    @Override
    public InterledgerFulfillment getFulfillment() {
      return EXECUTION_FULFILLMENT;
    }

    // Overriden because in the general case, `data` is only used when serialization occurs.
    @Override
    @Default
    public byte[] getData() {
      return new byte[0];
    }
  }
}