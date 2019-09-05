package org.interledger.link;

import org.interledger.core.InterledgerAddress;
import org.interledger.link.events.LinkConnectionEventEmitter;
import org.interledger.link.exceptions.LinkException;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * An implementation of {@link LinkFactory} for creating instances of {@link Link} that can handle `Unidirectional Ping`
 * packets.
 */
public class PingLoopbackLinkFactory implements LinkFactory {

  private final LinkConnectionEventEmitter linkConnectionEventEmitter;
  private PingLoopbackStatefulLink lazyPingLoopbackLink;

  /**
   * Required-args Constructor.
   */
  public PingLoopbackLinkFactory(final LinkConnectionEventEmitter linkConnectionEventEmitter) {
    this.linkConnectionEventEmitter = Objects.requireNonNull(linkConnectionEventEmitter, "linkEventEmitter must not be null");
  }

  /**
   * Construct a new instance of {@link Link} using the supplied inputs.
   *
   * @return A newly constructed instance of {@link Link}.
   */
  public Link<?> constructLink(
      final Supplier<Optional<InterledgerAddress>> operatorAddressSupplier, final LinkSettings linkSettings
  ) {
    Objects.requireNonNull(operatorAddressSupplier, "operatorAddressSupplier must not be null");
    Objects.requireNonNull(linkSettings, "linkSettings must not be null");

    if (!this.supports(linkSettings.getLinkType())) {
      throw new LinkException(
          String.format("LinkType not supported by this factory. linkType=%s", linkSettings.getLinkType()),
          LinkId.of("n/a")
      );
    }

    if (lazyPingLoopbackLink != null) {
      return lazyPingLoopbackLink;
    } else {
      synchronized (this) {
        if (lazyPingLoopbackLink == null) {
          lazyPingLoopbackLink = new PingLoopbackStatefulLink(operatorAddressSupplier, linkSettings,
              linkConnectionEventEmitter);
        }
        return lazyPingLoopbackLink;
      }
    }
  }

  @Override
  public boolean supports(LinkType linkType) {
    return PingLoopbackStatefulLink.LINK_TYPE.equals(linkType);
  }

}
