package org.interledger.node.events;

import org.interledger.annotations.Wrapped;
import org.interledger.annotations.Wrapper;

import org.immutables.value.Value;

import java.time.Instant;
import java.util.UUID;

/**
 * Base for all events
 */
public interface Event {

  @Value.Default
  default EventId getEventId() {
    return EventId.of(UUID.randomUUID());
  }

  @Value.Default
  default Instant getTimestamp() {
    return Instant.now();
  }

  /**
   * Identifier for {@link Event}.
   */
  @Wrapped
  abstract class WrappedEventId extends Wrapper<UUID> {

  }

}
