package org.interledger.link.events;

import org.interledger.link.Link;

/**
 * Handler interface that defines how to listen for events emitted by a {@link Link}.
 */
public interface LinkEventListener {

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

  /**
   * Called to handle an {@link LinkErrorEvent}.
   *
   * @param event A {@link LinkErrorEvent}.
   */
  default void onError(LinkErrorEvent event) {
    // No-op by default.
  }

}
