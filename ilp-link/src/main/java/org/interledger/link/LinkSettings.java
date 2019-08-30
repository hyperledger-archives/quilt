package org.interledger.link;

import org.immutables.value.Value;

import java.util.Map;

/**
 * Configuration information relating to a {@link Link}.
 */
public interface LinkSettings {

  static ImmutableLinkSettings.Builder builder() {
    return ImmutableLinkSettings.builder();
  }

  /**
   * The type of this ledger link.
   */
  LinkType getLinkType();

  /**
   * Additional, custom settings that any link can define.
   */
  Map<String, Object> getCustomSettings();

  @Value.Immutable
  abstract class AbstractLinkSettings implements LinkSettings {

    /**
     * Additional, custom settings that any link can define. Redacted to prevent credential leakage in log files.
     */
    @Value.Redacted
    public abstract Map<String, Object> getCustomSettings();

    /**
     * Always normalize Link-type String values to full uppercase to avoid casing ambiguity in properties files.
     */
    @Value.Check
    public AbstractLinkSettings normalize() {
      final String linkTypeString = this.getLinkType().value();
      if (!linkTypeString.toUpperCase().equals(linkTypeString)) {
        return ImmutableLinkSettings.builder()
          .from(this)
          .linkType(LinkType.of(linkTypeString.toUpperCase()))
          .build();
      } else {
        return this;
      }
    }

  }
}
