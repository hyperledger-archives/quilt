package org.interledger.link;

import org.interledger.link.exceptions.LinkException;

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A provider of {@link LinkFactory} scoped by type.
 */
public class LinkFactoryProvider {

  // At runtime, there will potentially be _many_ factories depending on the LinkType (e.g., a BTP Factory, a
  // LoopbackFactory, etc).
  private final Map<LinkType, LinkFactory> linkFactories;

  public LinkFactoryProvider() {
    this(Maps.newConcurrentMap());
  }

  public LinkFactoryProvider(final Map<LinkType, LinkFactory> linkFactories) {
    this.linkFactories = Objects.requireNonNull(linkFactories);
  }

  public LinkFactory getLinkFactory(final LinkType linkType) {
    Objects.requireNonNull(linkType, "linkType must not be null");
    return Optional.ofNullable(this.linkFactories.get(linkType))
        .orElseThrow(() -> new LinkException(
            String.format("No registered LinkFactory linkType=%s", linkType), LinkId.of("n/a"))
        );
  }

  public LinkFactory registerLinkFactory(final LinkType linkType, final LinkFactory linkFactory) {
    Objects.requireNonNull(linkType, "linkType must not be null");
    Objects.requireNonNull(linkFactory, "linkFactory must not be null");

    return this.linkFactories.put(linkType, linkFactory);
  }
}
