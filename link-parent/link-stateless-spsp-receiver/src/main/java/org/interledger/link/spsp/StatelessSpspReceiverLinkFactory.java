package org.interledger.link.spsp;

import org.interledger.core.InterledgerAddress;
import org.interledger.link.Link;
import org.interledger.link.LinkFactory;
import org.interledger.link.LinkId;
import org.interledger.link.LinkSettings;
import org.interledger.link.LinkType;
import org.interledger.link.PacketRejector;
import org.interledger.link.exceptions.LinkException;
import org.interledger.stream.receiver.StatelessStreamReceiver;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * An implementation of {@link LinkFactory} for constructing instances of {@link StatelessSpspReceiverLink}.
 */
public class StatelessSpspReceiverLinkFactory implements LinkFactory {

  private final PacketRejector packetRejector;
  private final StatelessStreamReceiver statelessStreamReceiver;

  /**
   * Required-args Constructor.
   *
   * @param packetRejector          An instance of {@link PacketRejector}.
   * @param statelessStreamReceiver A {@link StatelessStreamReceiver} for encrypting/decrypting STREAM packets.
   */
  public StatelessSpspReceiverLinkFactory(
      final PacketRejector packetRejector, final StatelessStreamReceiver statelessStreamReceiver
  ) {
    this.packetRejector = Objects.requireNonNull(packetRejector, "packetRejector must not be null");
    this.statelessStreamReceiver = Objects
        .requireNonNull(statelessStreamReceiver, "statelessStreamReceiver must not be null");
  }

  /**
   * An abstract method that sub-classes must implemented to provide actual factory functionality.
   *
   * @param operatorAddressSupplier A supplier for the ILP address of this node operating this Link. This value may be
   *                                uninitialized, for example, in cases where the Link obtains its address from a
   *                                parent node using IL-DCP. If an ILP address has not been assigned, or it has not
   *                                been obtained via IL-DCP, then this value will by default be {@link Link#SELF}.
   * @param linkSettings            An instance of {@link LinkSettings} to initialize this link from.
   *
   * @return A newly constructed instance of {@link Link}.
   */
  @Override
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

    // Translate from Link.customSettings, being sure to apply custom settings from the incoming link.
    final ImmutableStatelessSpspReceiverLinkSettings.Builder builder = StatelessSpspReceiverLinkSettings
        .builder()
        .from(linkSettings);

    final StatelessSpspReceiverLinkSettings statelessSpspReceiverLinkSettings =
        StatelessSpspReceiverLinkSettings.applyCustomSettings(builder, linkSettings.getCustomSettings()).build();

    return new StatelessSpspReceiverLink(
        operatorAddressSupplier, statelessSpspReceiverLinkSettings, statelessStreamReceiver
    );
  }

  @Override
  public boolean supports(LinkType linkType) {
    return StatelessSpspReceiverLink.LINK_TYPE.equals(linkType);
  }

}
