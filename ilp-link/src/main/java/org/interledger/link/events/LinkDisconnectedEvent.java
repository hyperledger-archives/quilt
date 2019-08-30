package org.interledger.link.events;

import org.interledger.link.Link;

import org.immutables.value.Value;

/**
 * Emitted after a ledger lpi2 disconnects from its peer.
 */
public interface LinkDisconnectedEvent extends LinkEvent {

  static LinkDisconnectedEvent of(final Link<?> link) {
    return ImmutableLinkDisconnectedEvent.builder().link(link).build();
  }

  @Value.Immutable
  abstract class AbstractLinkDisconnectedEvent implements
    LinkDisconnectedEvent {

  }

}
