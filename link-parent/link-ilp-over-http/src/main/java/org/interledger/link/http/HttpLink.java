package org.interledger.link.http;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.link.AbstractLink;
import org.interledger.link.Link;
import org.interledger.link.LinkType;
import org.interledger.link.events.LinkEventEmitter;

import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * An extension of {@link AbstractLink} that handles HTTP (aka, ILP over HTTP) connections, both incoming and outgoing.
 *
 * @see "https://github.com/interledger/rfcs/blob/master/0035-ilp-over-http/0035-ilp-over-http.md"
 */
public class HttpLink extends AbstractLink<HttpLinkSettings> implements Link<HttpLinkSettings> {

  public static final String LINK_TYPE_STRING = "HTTP";
  public static final LinkType LINK_TYPE = LinkType.of(LINK_TYPE_STRING);

  // Note: The RestTemplate in this sender is shared between all links...
  private HttpSender httpSender;

  /**
   * Required-args Constructor.
   *
   * @param httpLinkSettings A {@link HttpLinkSettings} that specified ledger link options.
   * @param linkEventEmitter A {@link LinkEventEmitter} that is used to emit events from this link.
   * @param httpSender       A {@link HttpSender} used to send messages with the remote HTTP peer.
   * @param linkEventEmitter A {@link LinkEventEmitter}.
   */
  public HttpLink(
      final Supplier<Optional<InterledgerAddress>> operatorAddressSupplier,
      final HttpLinkSettings httpLinkSettings,
      final HttpSender httpSender,
      final LinkEventEmitter linkEventEmitter
  ) {
    super(operatorAddressSupplier, httpLinkSettings, linkEventEmitter);
    this.httpSender = Objects.requireNonNull(httpSender);
  }

  /**
   * Perform the logic of actually connecting to the remote peer.
   */
  @Override
  public CompletableFuture<Void> doConnect() {
    // No-op.
    return CompletableFuture.supplyAsync(() -> {
      // If the peer is not up, the server operating this link will warn, but will not fail. One side needs to
      // startup first, so it's likely that this test will fail for the first side to startup, but can be useful for
      // connection debugging.
      httpSender.testConnection();
      return null;
    });
  }

  /**
   * Perform the logic of disconnecting from the remote peer.
   */
  @Override
  public CompletableFuture<Void> doDisconnect() {
    // No-op.
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public InterledgerResponsePacket sendPacket(final InterledgerPreparePacket preparePacket) {
    Objects.requireNonNull(preparePacket);

    return httpSender.sendPacket(preparePacket);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    HttpLink httpLink = (HttpLink) o;

    return httpSender.equals(httpLink.httpSender);
  }

  @Override
  public int hashCode() {
    return httpSender.hashCode();
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", HttpLink.class.getSimpleName() + "[", "]")
        .add("httpSender=" + httpSender)
        .toString();
  }
}
