package org.interledger.ildcp;

import org.interledger.annotations.Immutable;
import org.interledger.core.InterledgerCondition;

import org.immutables.value.Value.Default;

import java.math.BigInteger;
import java.time.Instant;

/**
 * A request to a Connector to get child account information.
 */
public interface IldcpRequest {

  InterledgerCondition EXECUTION_CONDITION = IldcpResponsePacket.EXECUTION_FULFILLMENT.getCondition();

  /**
   * Get the default builder.
   *
   * @return a {@link IldcpRequestBuilder} instance.
   */
  static IldcpRequestBuilder builder() {
    return new IldcpRequestBuilder();
  }

  /**
   * The destination of an ILP packet for IL-DCP is <tt>0</tt> by default, but can be adjusted.
   */
  default BigInteger getAmount() {
    return BigInteger.ZERO;
  }

  /**
   * The Date and time when the packet expires. Each connector changes the value of this field to set the expiry to an
   * earlier time, before forwarding the packet.
   *
   * @return The {@link Instant} this packet should be considered to be expired.
   */
  default Instant getExpiresAt() {
    return Instant.now().plusSeconds(30);
  }

  /**
   * Exists to satisfy Immutables.
   */
  @Immutable
  abstract class AbstractIldcpRequest implements IldcpRequest {

    @Override
    @Default
    public BigInteger getAmount() {
      return BigInteger.ZERO;
    }

    @Override
    @Default
    public Instant getExpiresAt() {
      return Instant.now().plusSeconds(30);
    }
  }

}
