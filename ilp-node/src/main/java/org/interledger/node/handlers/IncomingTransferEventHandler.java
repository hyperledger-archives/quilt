package org.interledger.node.handlers;

import org.interledger.node.events.IncomingTransferEvent;

/**
 * Handler interface for transfer events.
 */
@FunctionalInterface
public interface IncomingTransferEventHandler {

  /**
   * Called to handle an {@link IncomingTransferEvent}.
   *
   * @param event A {@link IncomingTransferEvent}.
   */
  void onTransfer(IncomingTransferEvent event);

}
