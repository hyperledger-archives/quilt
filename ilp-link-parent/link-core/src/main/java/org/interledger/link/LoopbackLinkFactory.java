package org.interledger.link;

import org.interledger.core.InterledgerAddress;
import org.interledger.link.events.LinkEventEmitter;

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
    this.linkEventEmitter = Objects.requireNonNull(linkEventEmitter);
    this.packetRejector = Objects.requireNonNull(packetRejector);
  }

  /**
   * Construct a new instance of {@link Link} using the supplied inputs.
   *
   * @return A newly constructed instance of {@link Link}.
   */
  public Link<?> constructLink(
    final Supplier<Optional<InterledgerAddress>> operatorAddressSupplier, final LinkSettings linkSettings
  ) {
    Objects.requireNonNull(linkSettings);

    if (!this.supports(linkSettings.getLinkType())) {
      throw new RuntimeException(
        String.format("LinkType `%s` not supported by this factory!", linkSettings.getLinkType())
      );
    }

    return new LoopbackLink(operatorAddressSupplier, linkSettings, linkEventEmitter, packetRejector);
  }

  @Override
  public boolean supports(LinkType linkType) {
    return LoopbackLink.LINK_TYPE.equals(linkType);
  }

}
