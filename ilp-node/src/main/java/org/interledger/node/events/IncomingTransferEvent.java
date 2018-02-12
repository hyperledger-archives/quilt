package org.interledger.node.events;

import org.interledger.annotations.Immutable;

/**
 * Emitted when an account channels encounters an error.
 */
public interface IncomingTransferEvent extends Event {

  static IncomingTransferEventBuilder builder() {
    return new IncomingTransferEventBuilder();
  }

  long getTransferAmount();

  @Immutable
  abstract class AbstractIncomingTransferEvent implements IncomingTransferEvent {

  }
}
