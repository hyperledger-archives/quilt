package org.interledger.core;

import java.util.Objects;

/**
 * A helper class for mapping an instance {@link InterledgerResponsePacket} according to the actual polymorphic type of
 * the passed-in instantiated object.
 *
 * @param <T> The response type to emit after mapping a particular packet.
 */
public abstract class InterledgerResponsePacketMapper<T> {

  /**
   * Handle the supplied {@code responsePacket} in a type-safe manner.
   *
   * @param responsePacket The generic {@link InterledgerResponsePacket} to be mapped in a type-safe manner.
   *
   * @return An instance of {@link T}.
   */
  public final T map(final InterledgerResponsePacket responsePacket) {
    Objects.requireNonNull(responsePacket);

    if (InterledgerFulfillPacket.class.isAssignableFrom(responsePacket.getClass())) {
      return mapFulfillPacket((InterledgerFulfillPacket) responsePacket);
    } else if (InterledgerRejectPacket.class.isAssignableFrom(responsePacket.getClass())) {
      return mapRejectPacket((InterledgerRejectPacket) responsePacket);
    } else {
      throw new RuntimeException(
          String.format("Unsupported InterledgerResponsePacket Type: %s", responsePacket.getClass()));
    }
  }

  /**
   * Handle the packet as an {@link InterledgerPacket}.
   *
   * @param interledgerFulfillPacket The generic {@link InterledgerPacket} to be mapped in a type-safe manner.
   *
   * @return An instance of {@link T}.
   */
  protected abstract T mapFulfillPacket(final InterledgerFulfillPacket interledgerFulfillPacket);

  /**
   * Handle the packet as an {@link InterledgerPacket}.
   *
   * @param interledgerRejectPacket The generic {@link InterledgerPacket} to be mapped in a type-safe manner.
   *
   * @return An instance of {@link T}.
   */
  protected abstract T mapRejectPacket(final InterledgerRejectPacket interledgerRejectPacket);
}
