package org.interledger.node.events;

import org.interledger.annotations.Immutable;
import org.interledger.core.InterledgerPreparePacket;

/**
 * Emitted when an account channels encounters an error.
 */
public interface IncomingRequestEvent extends Event {

  static IncomingRequestEventBuilder builder() {
    return new IncomingRequestEventBuilder();
  }

  InterledgerPreparePacket getRequest();

  @Immutable
  abstract class AbstractIncomingRequestEvent implements IncomingRequestEvent {

  }
}
