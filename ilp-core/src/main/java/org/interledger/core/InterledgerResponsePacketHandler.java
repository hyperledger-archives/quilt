package org.interledger.core;

import java.util.Objects;
import java.util.Optional;

/**
 * A helper class for mapping an instance {@link InterledgerResponsePacket} according to the actual polymorphic type of
 * the passed-in instantiated object.
 */
public abstract class InterledgerResponsePacketHandler {

  /**
   * Handle the supplied {@code responsePacket} in a type-safe manner.
   *
   * @param responsePacket The generic {@link InterledgerResponsePacket} to be mapped in a type-safe manner.
   */
  public final void handle(final Optional<InterledgerResponsePacket> responsePacket) {
    Objects.requireNonNull(responsePacket);

    if (responsePacket.isPresent()) {
      responsePacket
          .ifPresent($ -> {
            if (InterledgerFulfillPacket.class.isAssignableFrom($.getClass())) {
              handleFulfillPacket((InterledgerFulfillPacket) $);
            } else if (InterledgerRejectPacket.class.isAssignableFrom($.getClass())) {
              handleRejectPacket((InterledgerRejectPacket) $);
            } else {
              throw new RuntimeException(String.format("Unsupported InterledgerResponsePacket Type: %s", $.getClass()));
            }
          });
    } else {
      handleExpiredPacket();
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

  /**
   * Handle the packet as an {@link InterledgerPacket}.
   */
  protected abstract void handleExpiredPacket();

}
