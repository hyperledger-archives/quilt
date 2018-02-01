package org.interledger.node.handlers;

import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;

import java.util.concurrent.Future;

/**
 * Handler interface for request events.
 */
@FunctionalInterface
public interface IncomingRequestHandler {

  /**
   * Called to handle an incoming {@link InterledgerPreparePacket} and return
   * an {@link InterledgerFulfillPacket}.
   *
   * <p>If the upstream channel rejects the packet the handler MUST throw a new
   * {@link org.interledger.core.InterledgerProtocolException} with an attached
   * {@link org.interledger.core.InterledgerRejectPacket}.
   *
   * @param request A {@link InterledgerPreparePacket}.
   */
  Future<InterledgerFulfillPacket> onRequest(InterledgerPreparePacket request);

}
