package org.interledger.stream.pay.filters;

import org.interledger.core.fluent.FluentCompareTo;
import org.interledger.stream.pay.exceptions.StreamPayerException;
import org.interledger.stream.pay.filters.chain.StreamPacketFilterChain;
import org.interledger.stream.pay.model.ModifiableStreamPacketRequest;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.model.StreamPacketReply;
import org.interledger.stream.pay.model.StreamPacketRequest;
import org.interledger.stream.pay.trackers.PaymentSharedStateTracker;

import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;

import java.util.Objects;

/**
 * Determines how the maximum packet amount is known or discovered.
 */
public class SequenceFilter implements StreamPacketFilter {

  // Static because this filter will be constructed a lot.
  // private static final Logger LOGGER = LoggerFactory.getLogger(SequenceFilter.class);

  private static final UnsignedLong PACKET_LIMIT = UnsignedLong.valueOf(
    UnsignedInteger.MAX_VALUE.longValue() // <-- 2^32
  );

  private final PaymentSharedStateTracker paymentSharedStateTracker;

  /**
   * Required-args Constructor.
   *
   * @param paymentSharedStateTracker An instance of {@link PaymentSharedStateTracker}.
   */
  public SequenceFilter(final PaymentSharedStateTracker paymentSharedStateTracker) {
    this.paymentSharedStateTracker = Objects.requireNonNull(paymentSharedStateTracker);
  }

  @Override
  public SendState nextState(final ModifiableStreamPacketRequest modifiableStreamPacketRequest) {
    Objects.requireNonNull(modifiableStreamPacketRequest);

    // Obtain a sequence for this packet.
    final UnsignedLong nextSequence = paymentSharedStateTracker.getStreamConnection().nextSequence();
    modifiableStreamPacketRequest.setSequence(nextSequence);

    // Destroy the connection after 2^32 packets are sent for encryption safety:
    // https://github.com/interledger/rfcs/blob/master/0029-stream/0029-stream.md
    // #513-maximum-number-of-packets-per-connection
    if (FluentCompareTo.is(nextSequence).greaterThanEqualTo(PACKET_LIMIT)) {
      final String errorMessage = String.format(
        "Ending payment (cannot exceed max safe sequence number). streamConnection=%s",
        paymentSharedStateTracker.getStreamConnection()
      );
      throw new StreamPayerException(errorMessage, SendState.ExceededMaxSequence);
    }

    return SendState.Ready;
  }

  @Override
  public StreamPacketReply doFilter(
    final StreamPacketRequest streamRequest,
    final StreamPacketFilterChain filterChain
  ) {
    Objects.requireNonNull(streamRequest);
    Objects.requireNonNull(filterChain);

    return filterChain.doFilter(streamRequest);
  }

}
