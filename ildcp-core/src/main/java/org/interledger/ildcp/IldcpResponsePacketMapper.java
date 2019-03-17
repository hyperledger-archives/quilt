package org.interledger.ildcp;

import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;

import java.util.Objects;

/**
 * A helper class for mapping an instance {@link IldcpResponsePacket} according to the actual polymorphic type of the
 * passed-in instantiated object.
 *
 * @param <T> The response type to emit after mapping a particular packet.
 */
public abstract class IldcpResponsePacketMapper<T> {

  /**
   * Handle the supplied {@code responsePacket} in a type-safe manner.
   *
   * @param responsePacket The generic {@link InterledgerResponsePacket} to be mapped in a type-safe manner for IL-DCP
   *                       purposes.
   *
   * @return An instance of {@link T}.
   */
  public final T map(final InterledgerResponsePacket responsePacket) {
    Objects.requireNonNull(responsePacket);

    if (InterledgerFulfillPacket.class.isAssignableFrom(responsePacket.getClass())) {
      return mapFulfillPacket((IldcpResponsePacket) responsePacket);
    } else if (InterledgerRejectPacket.class.isAssignableFrom(responsePacket.getClass())) {
      return mapRejectPacket((InterledgerRejectPacket) responsePacket);
    } else {
      throw new RuntimeException(
          String.format("Unsupported IldcpResponsePacket Type: %s", responsePacket.getClass()));
    }
  }

  /**
   * Handle the packet as an {@link InterledgerPacket}.
   *
   * @param ildcpResponsePacket A {@link IldcpResponsePacket} to be mapped in a type-safe manner.
   *
   * @return An instance of {@link T}.
   */
  protected abstract T mapFulfillPacket(final IldcpResponsePacket ildcpResponsePacket);

  /**
   * Handle the packet as an {@link InterledgerPacket}.
   *
   * @param ildcpRejectPacket A {@link InterledgerRejectPacket} to be mapped in a type-safe manner.
   *
   * @return An instance of {@link T}.
   */
  protected abstract T mapRejectPacket(final InterledgerRejectPacket ildcpRejectPacket);
}
