package org.interledger.stream.pay.filters.chain;

import org.interledger.core.InterledgerPreparePacket;
import org.interledger.link.Link;
import org.interledger.stream.pay.model.ModifiableStreamPacketRequest;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.model.StreamPacketReply;
import org.interledger.stream.pay.model.StreamPacketRequest;

// TODO: FIXME Javadoc!

/**
 * <p>Provides a view into the invocation chain of a filtered Interledger Prepare/Fulfill/Reject request/response
 * flow.</p>
 *
 * <p>Filters use the this contract to invoke the next filter in the chain, or if the calling filter is the last filter
 * in the chain, to invoke {@link Link#sendPacket(InterledgerPreparePacket)} on the appropriate outbound link, which
 * exists at the end of the chain.</p>
 *
 * <p>An example filter chain for a typical Interledger Connector looks like this:</p>
 *
 * <pre>
 *   ┌────────────────────────────────────┐
 *   │   AllowedDestinationPacketFilter   │
 *   └────△───────────────────────────┬───┘
 * Fulfill/Reject                 Prepare
 *   ┌────┴───────────────────────────▽───┐
 *   │       MaxPacketAmountFilter        │
 *   └────△───────────────────────────┬───┘
 * Fulfill/Reject                 Prepare
 *   ┌────┴───────────────────────────▽───┐
 *   │         ExpiryPacketFilter         │
 *   └────△───────────────────────────┬───┘
 * Fulfill/Reject                 Prepare
 *   ┌────┴───────────────────────────▽───┐
 *   │       BalanceIlpPacketFilter       │
 *   └────△───────────────────────────┬───┘
 * Fulfill/Reject                 Prepare
 *   ┌────┴───────────────────────────▽───┐
 *   │  ValidateFulfillmentPacketFilter   │
 *   └────△───────────────────────────┬───┘
 * Fulfill/Reject                 Prepare
 *   ┌────┴───────────────────────────▽───┐
 *   │      PeerProtocolPacketFilter      │
 *   └────△───────────────────────────┬───┘
 * Fulfill/Reject                 Prepare
 *   ┌────┴───────────────────────────▽──┐
 *   │                                   │
 *   │           PacketSwitch            │
 *   │                                   │
 *   └───────────────────────────────────┘
 * </pre>
 **/
public interface StreamPacketFilterChain {

  /**
   * Signal if sending should continue and iteratively compose the next packet.
   * <p>
   * <ul>
   *   <li>Any controller can choose to immediately end the entire STREAM payment with an error, or choose to wait
   *   before sending the next packet.</li>
   *   <li>Note: the packet may not be sent if other controllers decline, so don't apply side effects in this method.</li>
   * </ul>
   * <p>
   *
   * @param modifiableStreamPacketRequest A {@link ModifiableStreamPacketRequest} that will ultimately be used to
   *                                      construct the next ILP Prepare and STREAM request.
   * @return A {@link SendState} corresponding the the next state.
   */
  SendState nextState(final ModifiableStreamPacketRequest modifiableStreamPacketRequest);

  /**
   * Apply side effects before sending an ILP Prepare over STREAM. Note that {@link #doFilter} is called synchronously
   * after all invocations of {@link #nextState} for all controllers. * @param request
   * <p>
   * Applies side-effects before sending an ILP prepare and after receiving a reject/fulfill.
   *
   * @param streamRequest The {@link StreamPacketRequest} containing finalized amounts and data for the ILP Prepare.
   * @return An optionally-present ILP response packet.
   */
  StreamPacketReply doFilter(StreamPacketRequest streamRequest);

}
