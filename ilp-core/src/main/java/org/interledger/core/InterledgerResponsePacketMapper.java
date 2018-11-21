package org.interledger.core;

import java.util.Objects;
import java.util.Optional;

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
  public final T map(final Optional<InterledgerResponsePacket> responsePacket) {
    Objects.requireNonNull(responsePacket);

    return responsePacket
        .map($ -> {
          if (InterledgerFulfillPacket.class.isAssignableFrom($.getClass())) {
            return mapFulfillPacket((InterledgerFulfillPacket) $);
          } else if (InterledgerRejectPacket.class.isAssignableFrom($.getClass())) {
            return mapRejectPacket((InterledgerRejectPacket) $);
          } else {
            throw new RuntimeException(
                String.format("Unsupported InterledgerResponsePacket Type: %s", $.getClass()));
          }
        })
        .orElseGet(this::mapExpiredPacket);
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

  /**
   * Handle the packet as an {@link InterledgerPacket}.
   */
  protected abstract T mapExpiredPacket();
}
