package org.interledger.node.handlers;

import org.interledger.node.events.ChannelOpenedEvent;

/**
 * Handler interface for connect events.
 */
@FunctionalInterface
public interface ChannelOpenedEventHandler {

  /**
   * Called to handle an {@link ChannelOpenedEvent}.
   *
   * @param event A {@link ChannelOpenedEvent}.
   */
  void onOpen(ChannelOpenedEvent event);

}
