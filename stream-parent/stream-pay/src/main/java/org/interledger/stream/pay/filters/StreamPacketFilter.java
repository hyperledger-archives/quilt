package org.interledger.stream.pay.filters;

import org.interledger.stream.pay.filters.chain.StreamPacketFilterChain;
import org.interledger.stream.pay.model.ModifiableStreamPacketRequest;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.model.StreamPacketReply;
import org.interledger.stream.pay.model.StreamPacketRequest;

/**
 * A filter mechanism for filtering stream packets. Filters should be stateless so that they can be created once, and
 * re-used across multiple run-loop invocations. If state is required in a filter, then a tracker (see {@link
 * org.interledger.stream.pay.trackers}) should be created that can track some state throughout a payment.
 */
public interface StreamPacketFilter {

  /**
   * Signal if sending should continue and iteratively compose the next packet.
   * <p>
   * <ul>
   *   <li>Any controller can choose to immediately end the entire STREAM payment with an error, or choose to wait
   *   before sending the next packet.</li>
   *   <li>Note: the packet may not be sent if other controllers decline, so don't apply side effects in this
   *   method.</li>
   * </ul>
   * <p>
   *
   * @param streamPacketRequest A {@link ModifiableStreamPacketRequest} that can be mutated by any filter, and that will
   *                            ultimately be used to construct the next ILP Prepare and STREAM packets for each request
   *                            that a run-loop will send to a receiver.
   *
   * @return A {@link SendState} corresponding the the next state.
   */
  SendState nextState(final ModifiableStreamPacketRequest streamPacketRequest);

  /**
   * Apply side effects before sending an ILP Prepare over STREAM. Note that {@link #doFilter} is called synchronously
   * after all invocations of {@link #nextState} for all controllers. * @param request
   * <p>
   * Applies side-effects before sending an ILP prepare and after receiving a reject/fulfill.
   *
   * @param streamPacketRequest The {@link StreamPacketRequest} containing finalized amounts and data for the ILP
   *                            Prepare.
   * @param filterChain         The {@link StreamPacketFilterChain} that this filter is operating inside of.
   *
   * @return An optionally-present ILP response packet.
   */
  StreamPacketReply doFilter(StreamPacketRequest streamPacketRequest, StreamPacketFilterChain filterChain);

}
