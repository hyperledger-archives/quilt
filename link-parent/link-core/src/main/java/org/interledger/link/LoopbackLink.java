package org.interledger.link;

import static org.interledger.core.InterledgerConstants.ALL_ZEROS_FULFILLMENT;

import com.google.common.annotations.VisibleForTesting;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.link.exceptions.LinkHandlerAlreadyRegisteredException;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * <p>A {@link Link} that always responds with a Fulfillment that contains the data supplied by the Prepare packet.</p>
 */
public class LoopbackLink extends AbstractLink<LinkSettings> implements Link<LinkSettings> {

  public static final String LINK_TYPE_STRING = "LOOPBACK";
  public static final LinkType LINK_TYPE = LinkType.of(LINK_TYPE_STRING);

  public static final InterledgerFulfillment LOOPBACK_FULFILLMENT = ALL_ZEROS_FULFILLMENT;

  // A constant key that can be added to this link's customSettings to simulate a particular rejection error code.
  public static final String SIMULATED_REJECT_ERROR_CODE = "simulatedRejectErrorCode";

  private final PacketRejector packetRejector;

  /**
   * Required-Args Constructor.
   *
   * @param operatorAddressSupplier A supplier for the ILP address of this node operating this Link. This value may be
   *                                uninitialized, for example, in cases where the Link obtains its address from a
   *                                parent node using IL-DCP. If an ILP address has not been assigned, or it has not
   *                                been obtained via IL-DCP, then this value will by default be {@link Link#SELF}.
   * @param linkSettings            A {@link LinkSettings} for this Link.
   * @param packetRejector          A {@link PacketRejector} to aid in rejecting packets in a uniform manner.
   */
  public LoopbackLink(
      final Supplier<InterledgerAddress> operatorAddressSupplier,
      final LinkSettings linkSettings,
      final PacketRejector packetRejector
  ) {
    super(operatorAddressSupplier, linkSettings);
    this.packetRejector = Objects.requireNonNull(packetRejector);
  }

  @Override
  public void registerLinkHandler(LinkHandler ilpDataHandler) throws LinkHandlerAlreadyRegisteredException {
    throw new RuntimeException(
        "Loopback links never have incoming data, and thus should not have a registered DataHandler."
    );
  }

  @Override
  public InterledgerResponsePacket sendPacket(final InterledgerPreparePacket preparePacket) {
    Objects.requireNonNull(preparePacket, "preparePacket must not be null");

    return Optional.ofNullable(this.getLinkSettings().getCustomSettings().get(SIMULATED_REJECT_ERROR_CODE))
        .map((value) -> {
          if (value.equals("T02")) {
            return packetRejector.reject(this.getLinkId(), preparePacket, InterledgerErrorCode.T02_PEER_BUSY,
                "Loopback set to manually reject via simulate_timeout=T02");
          } else if (value.equals("T03")) {
            return sleepAndReject(preparePacket, 60000);
          }
          if (value.equals("T99")) {
            throw new RuntimeException("T99 APPLICATION ERROR");
          } else {
            return InterledgerFulfillPacket.builder()
                .fulfillment(LOOPBACK_FULFILLMENT)
                .data(preparePacket.getData())
                .build();
          }
        })
        .orElseGet(InterledgerFulfillPacket.builder()
            .fulfillment(LOOPBACK_FULFILLMENT)
            .data(preparePacket.getData())::build);
  }

  @VisibleForTesting
  InterledgerResponsePacket sleepAndReject(InterledgerPreparePacket preparePacket, int sleepDuraction) {
    // Sleep for 1 minute, which in the typical case will exceed the Circuit-breaker's threshold.
    try {
      Thread.sleep(sleepDuraction);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    return packetRejector.reject(this.getLinkId(), preparePacket, InterledgerErrorCode.T03_CONNECTOR_BUSY,
        "Loopback set to exceed timeout via simulate_timeout=T03");
  }
}
