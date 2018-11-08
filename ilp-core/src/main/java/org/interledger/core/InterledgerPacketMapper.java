package org.interledger.core;

import java.util.Objects;

/**
 * A helper class for mapping an instance {@link InterledgerPacket} according to the actual polymorphic type of the
 * passed-in instantiated object.
 *
 * @param <T> The response type to emit after mapping a particular packet.
 *
 * @deprecated Will go away in-favor of response mapper.
 */
@Deprecated
public abstract class InterledgerPacketMapper<T> {

  /**
   * Handle the supplied {@code interledgerPacket} in a type-safe manner.
   *
   * @param interledgerPacket The generic {@link InterledgerPacket} to be mapped in a type-safe manner.
   *
   * @return An instance of {@link T}.
   */
  public final T map(final InterledgerPacket interledgerPacket) {
    Objects.requireNonNull(interledgerPacket);

    if (InterledgerPreparePacket.class.isAssignableFrom(interledgerPacket.getClass())) {
      return mapPreparePacket((InterledgerPreparePacket) interledgerPacket);
    } else if (InterledgerFulfillPacket.class.isAssignableFrom(interledgerPacket.getClass())) {
      return mapFulfillPacket((InterledgerFulfillPacket) interledgerPacket);
    } else if (InterledgerRejectPacket.class.isAssignableFrom(interledgerPacket.getClass())) {
      return mapRejectPacket((InterledgerRejectPacket) interledgerPacket);
    } else {
      throw new RuntimeException(String.format("Unsupported InterledgerPacket Type: %s", interledgerPacket.getClass()));
    }

  }

  /**
   * Handle the packet as an {@link InterledgerPacket}.
   *
   * @param interledgerPreparePacket The generic {@link InterledgerPacket} to be mapped in a type-safe manner.
   *
   * @return An instance of {@link T}.
   */
  protected abstract T mapPreparePacket(final InterledgerPreparePacket interledgerPreparePacket);

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
