package org.interledger.link;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Tracks links that have successfully connected, whether client links making outbound connections or server links that
 * are created in response to an incoming server request.
 */
public interface ConnectedLinksManager {

  /**
   * Accessor for an optionally-present Link that support the specified account address.
   *
   * @param linkId The unique identifier for a Link.
   *
   * @return An optionally-present {@link Link}.
   */
  Optional<Link<?>> getConnectedLink(LinkId linkId);

  /**
   * Accessor for an optionally-present Link that supports the specified account address.
   *
   * @param linkId The unique identifier for a Link.
   *
   * @return An optionally-present {@link Link}.
   */
  default <PS extends LinkSettings, P extends Link<PS>> Optional<P> getConnectedLink(
      final Class<P> $, final LinkId linkId
  ) {
    Objects.requireNonNull(linkId);
    return this.getConnectedLink(linkId).map(link -> (P) link);
  }

  Link<?> putConnectedLink(final Link<?> link);

  CompletableFuture<Void> removeConnectedLink(final LinkId linkId);
}
