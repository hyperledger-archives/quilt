package org.interledger.node.events;

import org.interledger.annotations.Immutable;

/**
 * Emitted after an account channels disconnects from its underlying ledger.
 */
public interface ChannelClosedEvent extends Event {

  static ChannelClosedEventBuilder builder() {
    return new ChannelClosedEventBuilder();
  }

  @Immutable
  abstract class AbstractChannelClosedEvent implements ChannelClosedEvent {

  }
}
