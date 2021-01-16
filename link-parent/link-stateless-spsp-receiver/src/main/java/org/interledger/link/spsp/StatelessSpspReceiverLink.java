package org.interledger.link.spsp;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.link.AbstractLink;
import org.interledger.link.Link;
import org.interledger.link.LinkHandler;
import org.interledger.link.LinkType;
import org.interledger.link.exceptions.LinkHandlerAlreadyRegisteredException;
import org.interledger.stream.Denomination;
import org.interledger.stream.receiver.StreamReceiver;

import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Supplier;

/**
 * <p>A {@link Link} that attempts to fulfill packets using an SPSP receiver.</p>
 */
public class StatelessSpspReceiverLink extends AbstractLink<StatelessSpspReceiverLinkSettings>
  implements Link<StatelessSpspReceiverLinkSettings> {

  public static final String LINK_TYPE_STRING = "STATELESS_SPSP_RECEIVER";
  public static final LinkType LINK_TYPE = LinkType.of(LINK_TYPE_STRING);

  private final StreamReceiver streamReceiver;
  private final Denomination denomination;

  /**
   * Required-Args Constructor.
   *
   * @param operatorAddressSupplier A supplier for the ILP address of this node operating this Link. This value may be
   *                                uninitialized, for example, in cases where the Link obtains its address from a
   *                                parent node using IL-DCP. If an ILP address has not been assigned, or it has not
   *                                been obtained via IL-DCP, then this value will by default be {@link Link#SELF}.
   * @param linkSettings            A {@link StatelessSpspReceiverLinkSettings} for this Link.
   * @param streamReceiver          A {@link StreamReceiver} that can fulfill packets.
   */
  public StatelessSpspReceiverLink(
    final Supplier<InterledgerAddress> operatorAddressSupplier,
    final StatelessSpspReceiverLinkSettings linkSettings,
    final StreamReceiver streamReceiver
  ) {
    super(operatorAddressSupplier, linkSettings);
    this.denomination = Denomination.builder()
      .assetCode(linkSettings.assetCode())
      .assetScale((short) linkSettings.assetScale())
      .build();
    this.streamReceiver = Objects.requireNonNull(streamReceiver);
  }

  @Override
  public void registerLinkHandler(final LinkHandler ilpDataHandler) throws LinkHandlerAlreadyRegisteredException {
    throw new RuntimeException(
      "StatelessSpspReceiver links never emit data, and thus should not have a registered DataHandler."
    );
  }

  @Override
  public InterledgerResponsePacket sendPacket(final InterledgerPreparePacket preparePacket) {
    Objects.requireNonNull(preparePacket, "preparePacket must not be null");

    return streamReceiver.receiveMoney(preparePacket, this.getOperatorAddressSupplier().get(), this.denomination)
      .map(fulfillPacket -> {
          if (logger.isDebugEnabled()) {
            logger.debug("Packet fulfilled! preparePacket={} fulfillPacket={}", preparePacket, fulfillPacket);
          }
          return fulfillPacket;
        },
        rejectPacket -> {
          if (logger.isDebugEnabled()) {
            logger.debug("Packet rejected! preparePacket={} rejectPacket={}", preparePacket, rejectPacket);
          }
          return rejectPacket;
        }
      );
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", StatelessSpspReceiverLink.class.getSimpleName() + "[", "]")
      .add("linkId=" + getLinkId())
      .add("operatorAddressSupplier=" + getOperatorAddressSupplier().get())
      .add("streamReceiver=" + streamReceiver)
      .add("denomination=" + denomination)
      .add("linkSettings=" + getLinkSettings())
      .toString();
  }
}
