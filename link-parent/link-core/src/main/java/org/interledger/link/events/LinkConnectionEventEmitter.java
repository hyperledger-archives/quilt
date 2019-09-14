package org.interledger.link.events;

import org.interledger.link.Link;

import java.util.UUID;

/**
 * Defines how a link should emit connection-related events. Note that a given {@link Link} has only a single connection
 * event-emitter.
 */
public interface LinkConnectionEventEmitter {

  /**
   * Emit an event of type {@link LinkConnectedEvent}.
   *
   * @param event The event to emit.
   */
  void emitEvent(LinkConnectedEvent event);

  /**
   * Emit an event of type {@link LinkDisconnectedEvent}.
   *
   * @param event The event to emit.
   */
  void emitEvent(LinkDisconnectedEvent event);

  /**
   * Add a DataLink event listener to this emitter.
   *
   * @param eventlistener A {@link LinkConnectionEventListener} that can handle various types of events emitted by this
   *                      ledger link.
   */
  void addLinkConnectionEventListener(LinkConnectionEventListener eventlistener);

  /**
   * Removes an event listener from this emitter.
   *
   * @param eventlistener A {@link UUID} representing the unique identifier of the listener, as seen by this emitter.
   */
  void removeLinkConnectionEventListener(LinkConnectionEventListener eventlistener);
}
