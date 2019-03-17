package org.interledger.ildcp;

import org.interledger.core.InterledgerPacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.InterledgerRuntimeException;

import java.util.Objects;

/**
 * A helper class for mapping an instance {@link IldcpResponsePacket} according to the actual polymorphic type of the
 * passed-in instantiated object.
 */
public abstract class IldcpResponsePacketHandler {

  /**
   * Handle the supplied {@code responsePacket} in a type-safe manner.
   *
   * @param responsePacket The generic {@link InterledgerResponsePacket} to be mapped in a type-safe manner for IL-DCP
   *                       purposes.
   */
  public final void handle(final InterledgerResponsePacket responsePacket) {
    Objects.requireNonNull(responsePacket);

    if (IldcpResponsePacket.class.isAssignableFrom(responsePacket.getClass())) {
      handleIldcpResponsePacket((IldcpResponsePacket) responsePacket);
    } else if (InterledgerRejectPacket.class.isAssignableFrom(responsePacket.getClass())) {
      handleIldcpErrorPacket((InterledgerRejectPacket) responsePacket);
    } else {
      throw new RuntimeException(
          String.format("Unsupported IldcpResponsePacket Type: %s", responsePacket.getClass()));
    }
  }

  /**
   * Handle the packet as an {@link InterledgerPacket}.
   *
   * @param ildcpResponsePacket The generic {@link InterledgerPacket} typed as an instance of {@link
   *                            IldcpResponsePacket}, to be mapped in a type-safe manner.
   */
  protected abstract void handleIldcpResponsePacket(final IldcpResponsePacket ildcpResponsePacket);

  /**
   * Handle the packet as an {@link InterledgerPacket}.
   *
   * @param ildcpErrorPacket The generic {@link InterledgerRejectPacket} to be mapped in a type-safe manner.
   */
  protected abstract void handleIldcpErrorPacket(final InterledgerRejectPacket ildcpErrorPacket);

}
