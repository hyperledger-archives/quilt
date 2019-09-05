package org.interledger.link.events;

import org.interledger.link.Link;

import org.immutables.value.Value;

/**
 * Emitted after a {@link Link} disconnects from a remote peer.
 */
public interface LinkDisconnectedEvent extends LinkConnectionEvent {

  static LinkDisconnectedEvent of(final Link<?> link) {
    return ImmutableLinkDisconnectedEvent.builder().link(link).build();
  }

  @Value.Immutable
  abstract class AbstractLinkDisconnectedEvent implements
      LinkDisconnectedEvent {

  }

}
