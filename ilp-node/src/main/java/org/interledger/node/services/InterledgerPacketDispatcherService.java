package org.interledger.node.services;

import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRuntimeException;
import org.interledger.node.Account;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;


/**
 * Dispatches packets by looping through all handlers and dispathing to the first that can handle
 * the packet.
 */
public class InterledgerPacketDispatcherService implements InterledgerRequestHandler {

  final List<InterledgerRequestHandler> requestHandlers;

  public InterledgerPacketDispatcherService(InterledgerRequestHandler... requestHandlers) {
    this.requestHandlers = Arrays.asList(requestHandlers);
  }

  @Override
  public Future<InterledgerFulfillPacket> handlePacket(
      Account sourceAccount, InterledgerPreparePacket incomingRequest) {

    if (incomingRequest.getExpiresAt().isBefore(Instant.now())) {
      throw new InterledgerRuntimeException("Request packet has expired.");
    }

    for (InterledgerRequestHandler handler : requestHandlers) {
      if(handler.canHandlePacket(sourceAccount, incomingRequest)) {
        return handler.handlePacket(sourceAccount, incomingRequest);
      }
    }

    //TODO Better exceptions
    throw new InterledgerRuntimeException("No handler for this packet.");

  }

  @Override
  public boolean canHandlePacket(Account sourceAccount, InterledgerPreparePacket incomingRequest) {
    for (InterledgerRequestHandler handler : requestHandlers) {
      if(handler.canHandlePacket(sourceAccount, incomingRequest)) {
        return true;
      }
    }
    return false;
  }

}
