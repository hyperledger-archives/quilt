package org.interledger.stream.pay.filters;

import com.google.common.primitives.UnsignedInteger;
import java.util.Objects;
import org.interledger.core.fluent.FluentCompareTo;
import org.interledger.stream.pay.filters.chain.StreamPacketFilterChain;
import org.interledger.stream.pay.model.ModifiableStreamPacketRequest;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.model.StreamPacketReply;
import org.interledger.stream.pay.trackers.PaymentSharedStateTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Determines how the maximum packet amount is known or discovered.
 */
public class SequenceFilter implements StreamPacketFilter {

  // Static because these filters will be constructed a lot.
  private static final Logger LOGGER = LoggerFactory.getLogger(SequenceFilter.class.getClass());

  private static UnsignedInteger PACKET_LIMIT = UnsignedInteger.MAX_VALUE;

  private PaymentSharedStateTracker paymentSharedStateTracker;

  public SequenceFilter(final PaymentSharedStateTracker paymentSharedStateTracker) {
    this.paymentSharedStateTracker = Objects.requireNonNull(paymentSharedStateTracker);
  }

//  @Override
//  public SendState nextState(final ModifiableStreamPacketRequest modifiableStreamRequest) {
//    Objects.requireNonNull(modifiableStreamRequest);
//
//    return SendState.Ready;
//  }


  @Override
  public StreamPacketReply doFilter(
    final ModifiableStreamPacketRequest streamRequest, final StreamPacketFilterChain filterChain
  ) {
    Objects.requireNonNull(streamRequest);
    Objects.requireNonNull(filterChain);

    // Obtain a sequence for this packet.
    final UnsignedInteger nextSequence = paymentSharedStateTracker.getStreamConnection()
      .nextSequence();

    // Destroy the connection after 2^32 packets are sent for encryption safety:
    // https://github.com/interledger/rfcs/blob/master/0029-stream/0029-stream.md#513-maximum-number-of-packets-per-connection
    if (FluentCompareTo.is(nextSequence).greaterThanEqualTo(PACKET_LIMIT)) {
      LOGGER.error("Ending payment (cannot exceed max safe sequence number).");
      return StreamPacketReply.builder().sendState(SendState.ExceededMaxSequence).build();
    }

    streamRequest.setSequence(nextSequence);

    return filterChain.doFilter(streamRequest);
  }

}
