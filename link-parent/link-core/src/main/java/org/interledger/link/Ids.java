package org.interledger.link;

import org.interledger.core.Wrapped;
import org.interledger.core.Wrapper;

import org.immutables.value.Value;

/**
 * Wrapped immutable classes for providing type-safe identifiers.
 */
public class Ids {

  @Value.Immutable
  @Wrapped
  static abstract class _LinkId extends Wrapper<String> {
  }

  @Value.Immutable
  @Wrapped
  static abstract class _LinkType extends Wrapper<String> {

    /**
     * Always normalize Link-type String values to full uppercase to avoid casing ambiguity in properties files.
     *
     * @return A Link type.
     */
    @Value.Check
    public _LinkType normalize() {
      final String linkTypeString = this.value();
      if (!linkTypeString.toUpperCase().equals(linkTypeString)) {
        return LinkType.of(linkTypeString.toUpperCase());
      } else {
        return this;
      }
    }

  }
}
