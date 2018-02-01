package org.interledger.node.handlers;

import org.interledger.node.events.ChannelErrorEvent;

/**
 * Handler interface for error evets.
 */
@FunctionalInterface
public interface ChannelErrorEventHandler {

  /**
   * Called to handle an {@link ChannelErrorEvent}.
   *
   * @param event A {@link ChannelErrorEvent}.
   */
  void onError(ChannelErrorEvent event);

}
