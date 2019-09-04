package org.interledger.link.events;

import org.interledger.link.Link;

import org.immutables.value.Value;

/**
 * Emitted after a lpi2 connects to a remote peer.
 */
public interface LinkConnectedEvent extends LinkEvent {

  static LinkConnectedEvent of(final Link<?> link) {
    return ImmutableLinkConnectedEvent.builder().link(link).build();
  }

  @Value.Immutable
  abstract class AbstractLinkConnectedEvent implements
    LinkConnectedEvent {

  }

}
