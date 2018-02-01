package org.interledger.node.events;

import org.interledger.annotations.Immutable;

/**
 * Emitted when an account channels encounters an error.
 */
public interface ChannelErrorEvent extends Event {

  static ChannelErrorEventBuilder builder() {
    return new ChannelErrorEventBuilder();
  }

  Exception getError();

  @Immutable
  abstract class AbstractChannelErrorEvent implements ChannelErrorEvent{

  }

}
