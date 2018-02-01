package org.interledger.node.events;

import org.interledger.annotations.Immutable;

/**
 * Emitted after an account channels connects to its underlying ledger.
 */
public interface ChannelOpenedEvent extends Event {

  static ChannelOpenedEventBuilder builder() {
    return new ChannelOpenedEventBuilder();
  }

  @Immutable
  abstract class AbstractChannelOpenedEvent implements ChannelOpenedEvent {

  }

}
