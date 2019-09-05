package org.interledger.link.events;

import org.interledger.link.Link;

/**
 * Handler interface that defines how to listen for events emitted by a {@link Link}.
 */
public interface LinkConnectionEventListener {

  /**
   * Called to handle an {@link LinkConnectedEvent}.
   *
   * @param event A {@link LinkConnectedEvent}.
   */
  void onConnect(LinkConnectedEvent event);

  /**
   * Called to handle an {@link LinkDisconnectedEvent}.
   *
   * @param event A {@link LinkDisconnectedEvent}.
   */
  void onDisconnect(LinkDisconnectedEvent event);

}
