package org.interledger.link.events;

import org.interledger.link.Link;

import com.google.common.collect.Maps;
import org.immutables.value.Value.Default;

import java.util.Map;

/**
 * A parent interface for all link events.
 */
public interface LinkEvent {

  /**
   * Accessor for the DataLink that emitted this event.
   */
  Link<?> getLink();

  /**
   * Custom properties that can be added to any DataLink event.
   *
   * @return
   */
  @Default
  default Map<String, Object> getCustomSettings() {
    return Maps.newConcurrentMap();
  }

}
