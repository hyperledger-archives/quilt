package org.interledger.link.events;

import org.interledger.link.Link;

import java.util.UUID;

/**
 * Defines how a link should emit events. Note that a given {@link Link} has only a single event-emitter.
 */
public interface LinkEventEmitter {

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
   * Emit an event of type {@link LinkErrorEvent}.
   *
   * @param event The event to emit.
   */
  void emitEvent(LinkErrorEvent event);

  /**
   * Add a DataLink event listener to this emitter.
   *
   * @param eventlistener A {@link LinkEventListener} that can handle various types of events emitted by this ledger
   *                      link.
   *
   * @return A {@link UUID} representing the unique identifier of the listener, as seen by this ledger link.
   */
  void addLinkEventListener(LinkEventListener eventlistener);

  /**
   * Removes an event listener from this emitter.
   *
   * @param eventlistener A {@link UUID} representing the unique identifier of the listener, as seen by this emitter.
   */
  void removeLinkEventListener(LinkEventListener eventlistener);
}
