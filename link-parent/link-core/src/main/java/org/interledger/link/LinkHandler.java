package org.interledger.link;

import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;

import java.util.concurrent.CompletableFuture;

/**
 * Handles an incoming {@link InterledgerPreparePacket} for a single plugin, sent from a remote peer.
 */
@FunctionalInterface
public interface LinkHandler {

  /**
   * Handles an incoming {@link InterledgerPreparePacket} received from a connected peer, but that may have originated
   * from any Interledger sender in the network.
   *
   * @param incomingPreparePacket A {@link InterledgerPreparePacket} containing data about an incoming payment.
   *
   * @return A {@link CompletableFuture} that resolves to an optionally-present {@link InterledgerResponsePacket}, which
   *     will be of concrete type {@link InterledgerFulfillPacket} or {@link InterledgerRejectPacket}, if present.
   */
  InterledgerResponsePacket handleIncomingPacket(InterledgerPreparePacket incomingPreparePacket);
}
