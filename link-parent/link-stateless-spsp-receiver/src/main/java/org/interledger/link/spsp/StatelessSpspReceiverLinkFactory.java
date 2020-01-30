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

import com.google.common.base.Preconditions;

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

    Preconditions.checkArgument(
        StatelessSpspReceiverLinkSettings.class.isAssignableFrom(linkSettings.getClass()),
        "Constructing an instance of StatelessSpspReceiverLink requires an instance of StatelessSpspReceiverLinkSettings"
    );

    return new StatelessSpspReceiverLink(
        operatorAddressSupplier, (StatelessSpspReceiverLinkSettings) linkSettings, statelessStreamReceiver
    );
  }

  @Override
  public boolean supports(LinkType linkType) {
    return StatelessSpspReceiverLink.LINK_TYPE.equals(linkType);
  }

}
