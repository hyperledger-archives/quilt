package org.interledger.core;

import java.util.Objects;

/**
 * A helper class for mapping an instance {@link InterledgerResponsePacket} according to the actual polymorphic type of
 * the passed-in instantiated object.
 */
public abstract class InterledgerResponsePacketHandler {

  /**
   * Handle the supplied {@code interledgerResponsePacket} in a type-safe manner.
   *
   * @param interledgerResponsePacket The generic {@link InterledgerResponsePacket} to be mapped in a type-safe manner.
   */
  public final void handle(final InterledgerResponsePacket interledgerResponsePacket) {
    Objects.requireNonNull(interledgerResponsePacket);
    if (InterledgerFulfillPacket.class.isAssignableFrom(interledgerResponsePacket.getClass())) {
      handleFulfillPacket((InterledgerFulfillPacket) interledgerResponsePacket);
    } else if (InterledgerRejectPacket.class.isAssignableFrom(interledgerResponsePacket.getClass())) {
      handleRejectPacket((InterledgerRejectPacket) interledgerResponsePacket);
    } else {
      throw new RuntimeException(
          String.format("Unsupported InterledgerResponsePacket Type: %s", interledgerResponsePacket.getClass()));
    }
  }

  /**
   * Handle the packet as an {@link InterledgerPacket}.
   *
   * @param interledgerFulfillPacket The generic {@link InterledgerPacket} to be mapped in a type-safe manner.
   */
  protected abstract void handleFulfillPacket(final InterledgerFulfillPacket interledgerFulfillPacket);

  /**
   * Handle the packet as an {@link InterledgerPacket}.
   *
   * @param interledgerRejectPacket The generic {@link InterledgerPacket} to be mapped in a type-safe manner.
   */
  protected abstract void handleRejectPacket(final InterledgerRejectPacket interledgerRejectPacket);

}
