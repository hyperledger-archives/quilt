package org.interledger.link;

import org.interledger.core.InterledgerAddress;
import org.interledger.link.exceptions.LinkException;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * An implementation of {@link LinkFactory} for creating Links that can handle the `Loopback` packets.
 */
public class LoopbackLinkFactory implements LinkFactory {

  private final PacketRejector packetRejector;

  /**
   * Required-args Constructor.
   *
   * @param packetRejector An instance of {@link PacketRejector}.
   */
  public LoopbackLinkFactory(final PacketRejector packetRejector) {
    this.packetRejector = Objects.requireNonNull(packetRejector, "packetRejector must not be null");
  }

  /**
   * Construct a new instance of {@link Link} using the supplied inputs.
   *
   * @param operatorAddressSupplier A supplier for the ILP address of this node operating this Link. This value may be
   *                                uninitialized, for example, in cases where the Link obtains its address from a
   *                                parent node using IL-DCP. If an ILP address has not been assigned, or it has not
   *                                been obtained via IL-DCP, then this value will by default be {@link Link#SELF}.
   * @param linkSettings            An instance of {@link LinkSettings} to initialize this link from.
   *
   * @return A newly constructed instance of {@link Link}.
   */
  public Link<?> constructLink(
      final Supplier<InterledgerAddress> operatorAddressSupplier, final LinkSettings linkSettings
  ) {
    Objects.requireNonNull(operatorAddressSupplier, "operatorAddressSupplier must not be null");
    Objects.requireNonNull(linkSettings, "linkSettings must not be null");

    if (!this.supports(linkSettings.getLinkType())) {
      throw new LinkException(
          String.format("LinkType not supported by this factory. linkType=%s", linkSettings.getLinkType()),
          LinkId.of("n/a")
      );
    }

    return new LoopbackLink(operatorAddressSupplier, linkSettings, packetRejector);
  }

  @Override
  public boolean supports(LinkType linkType) {
    return LoopbackLink.LINK_TYPE.equals(linkType);
  }

}
