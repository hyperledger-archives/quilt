package org.interledger.node.handlers;

import org.interledger.node.events.ChannelClosedEvent;

/**
 * Handler interface that defines all events related to connecting/disconnecting.
 */
@FunctionalInterface
public interface ChannelClosedEventHandler {

  /**
   * Called to handle an {@link ChannelClosedEvent}.
   *
   * @param event A {@link ChannelClosedEvent}.
   */
  void onDisconnect(ChannelClosedEvent event);

}
