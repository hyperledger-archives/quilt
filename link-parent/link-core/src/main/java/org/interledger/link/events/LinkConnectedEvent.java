package org.interledger.link.events;

import org.interledger.link.Link;

import org.immutables.value.Value;

/**
 * Emitted after a {@link Link} connects to a remote peer.
 */
public interface LinkConnectedEvent extends LinkConnectionEvent {

  static LinkConnectedEvent of(final Link<?> link) {
    return ImmutableLinkConnectedEvent.builder().link(link).build();
  }

  @Value.Immutable
  abstract class AbstractLinkConnectedEvent implements
      LinkConnectedEvent {

  }

}
