package org.interledger.link;

import org.interledger.core.InterledgerAddress;
import org.interledger.link.events.LinkEventEmitter;
import org.interledger.link.exceptions.LinkException;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * An implementation of {@link LinkFactory} for creating Links that can handle the `Loopback` packets.
 */
public class LoopbackLinkFactory implements LinkFactory {

  private final LinkEventEmitter linkEventEmitter;
  private final PacketRejector packetRejector;

  /**
   * Required-args Constructor.
   */
  public LoopbackLinkFactory(final LinkEventEmitter linkEventEmitter, final PacketRejector packetRejector) {
    this.linkEventEmitter = Objects.requireNonNull(linkEventEmitter, "linkEventEmitter must not be null");
    this.packetRejector = Objects.requireNonNull(packetRejector, "packetRejector must not be null");
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

    return new LoopbackLink(operatorAddressSupplier, linkSettings, linkEventEmitter, packetRejector);
  }

  @Override
  public boolean supports(LinkType linkType) {
    return LoopbackLink.LINK_TYPE.equals(linkType);
  }

}
