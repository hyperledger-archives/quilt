package org.interledger.stream.pay.filters;

import org.interledger.stream.pay.filters.chain.StreamPacketFilterChain;
import org.interledger.stream.pay.model.ModifiableStreamPacketRequest;
import org.interledger.stream.pay.model.StreamPacketReply;
import org.interledger.stream.pay.model.StreamPacketRequest;

/**
 * A filter mechanism for filtering stream packets.
 */
public interface StreamPacketFilter {

  /**
   * Signal if sending should continue and iteratively compose the next packet. Any filter can choose to immediately end
   * the entire STREAM payment with an error, or choose to wait before sending the next packet.
   *
   * Note: the packet may not be sent if other controllers decline, so don't apply side effects.
   *
   * @param modifiableStreamRequest
   *
   * @return
   */
  //SendState nextState(final ModifiableStreamPacketRequest modifiableStreamRequest);

  /**
   * Applies side-effects before sending an ILP prepare and after receiving a reject/fulfill.
   *
   * @param streamRequest The {@link StreamPacketRequest} that this filter is operating upon.
   * @param filterChain   The {@link StreamPacketFilterChain} that this filter is operating inside of.
   *
   * @return An optionally-present ILP response packet.
   */
  StreamPacketReply doFilter(ModifiableStreamPacketRequest streamRequest, StreamPacketFilterChain filterChain);

}
