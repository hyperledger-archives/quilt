package org.interledger.link;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.link.exceptions.LinkHandlerAlreadyRegisteredException;

import java.util.Base64;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * <p>A {@link Link} that responds to Ping protocol packets that conform to the Ping protocol.</p>
 *
 * <p>Ping functionality is implemented as a Loopback Link so that all packet-processing related to balance tracking
 * can be properly performed without any special processing inside of the core connector logic. Additionally, this link
 * is a loopback link so that no outbound traffic ever leaves the Connector while processing a Ping request.
 * </p>
 */
public class PingLoopbackLink extends AbstractLink<LinkSettings> implements Link<LinkSettings> {

  public static final String LINK_TYPE_STRING = "PING";
  public static final LinkType LINK_TYPE = LinkType.of(LINK_TYPE_STRING);

  public static final InterledgerFulfillment PING_PROTOCOL_FULFILLMENT =
      InterledgerFulfillment.of(Base64.getDecoder().decode("cGluZ3BpbmdwaW5ncGluZ3BpbmdwaW5ncGluZ3Bpbmc="));
  public static final InterledgerCondition PING_PROTOCOL_CONDITION = InterledgerCondition.of(
      Base64.getDecoder().decode("jAC8DGFPZPfh4AtZpXuvXFe2oRmpDVSvSJg2oT+bx34="));

  /**
   * Required-Args Constructor.
   *
   * @param operatorAddressSupplier A supplier for the ILP address of this node operating this Link. This value may be
   *                                uninitialized, for example, in cases where the Link obtains its address from a
   *                                parent node using IL-DCP. If an ILP address has not been assigned, or it has not
   *                                been obtained via IL-DCP, then this value will by default be {@link Link#SELF}.
   * @param linkSettings            An instance of {@link LinkSettings} to initialize this link from.
   */
  public PingLoopbackLink(
      final Supplier<InterledgerAddress> operatorAddressSupplier, final LinkSettings linkSettings
  ) {
    super(operatorAddressSupplier, linkSettings);
  }

  @Override
  public void registerLinkHandler(LinkHandler ilpDataHandler) throws LinkHandlerAlreadyRegisteredException {
    throw new RuntimeException(
        "PingLoopback links never have incoming data, and thus should not have a registered DataHandler."
    );
  }

  @Override
  public InterledgerResponsePacket sendPacket(final InterledgerPreparePacket preparePacket) {
    Objects.requireNonNull(preparePacket, "preparePacket must not be null!");

    if (preparePacket.getExecutionCondition().equals(PING_PROTOCOL_CONDITION)) {
      return InterledgerFulfillPacket.builder()
          .fulfillment(PING_PROTOCOL_FULFILLMENT)
          .data(preparePacket.getData())
          .build();
    } else {
      // Reject.
      final InterledgerRejectPacket rejectPacket = InterledgerRejectPacket.builder()
          .triggeredBy(getOperatorAddressSupplier().get())
          .code(InterledgerErrorCode.F00_BAD_REQUEST)
          .message("Invalid Ping Protocol Condition")
          .build();

      logger.warn(
          "Rejecting Unidirectional Ping packet: PreparePacket={} RejectPacket={}", preparePacket, rejectPacket
      );

      return rejectPacket;
    }
  }
}
