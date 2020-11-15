package org.interledger.stream.pay.filters;

import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.stream.pay.filters.chain.StreamPacketFilterChain;
import org.interledger.stream.pay.model.ModifiableStreamPacketRequest;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.model.StreamPacketReply;
import org.interledger.stream.pay.model.StreamPacketRequest;
import org.interledger.stream.pay.trackers.MaxPacketAmountTracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Determines how the maximum packet amount is known or discovered.
 */
public class MaxPacketAmountFilter implements StreamPacketFilter {

  // Static because this filter will be constructed a lot.
  private static final Logger LOGGER = LoggerFactory.getLogger(MaxPacketAmountFilter.class);

  private final MaxPacketAmountTracker maxPacketAmountTracker;

  /**
   * Required-args Constructor.
   *
   * @param maxPacketAmountTracker A {@link MaxPacketAmountTracker}.
   */
  public MaxPacketAmountFilter(final MaxPacketAmountTracker maxPacketAmountTracker) {
    this.maxPacketAmountTracker = Objects.requireNonNull(maxPacketAmountTracker);
  }

  @Override
  public SendState nextState(final ModifiableStreamPacketRequest streamPacketRequest) {
    Objects.requireNonNull(streamPacketRequest);

    // Stop sending if no capacity is available.
    if (this.maxPacketAmountTracker.getNoCapacityAvailable()) {
      return SendState.ConnectorError;
    }

    return SendState.Ready;
  }

  @Override
  public StreamPacketReply doFilter(
    final StreamPacketRequest streamPacketRequest, final StreamPacketFilterChain filterChain
  ) {
    Objects.requireNonNull(streamPacketRequest);
    Objects.requireNonNull(filterChain);

    return filterChain.doFilter(streamPacketRequest)
      // Logging
      .handleAndReturn(
        streamPacketFulfill -> {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("streamPacketFulfill={}", streamPacketFulfill);
          }
        },
        streamPacketReject -> {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("streamPacketReject={}", streamPacketReject);
          }
        })
      //////////////////
      // Actual handling
      .handleAndReturn(
        streamPacketFulfill -> {
          if (streamPacketFulfill.isAuthentic()) {
            // Do for any authentic packet.
            this.maxPacketAmountTracker.adjustPathCapacity(streamPacketRequest.sourceAmount());
          }
        },
        streamPacketReject -> {
          // The rejectPacket should be here always, but just in case.
          streamPacketReject.interledgerResponsePacket()
            .filter(packet -> InterledgerRejectPacket.class.isAssignableFrom(packet.getClass()))
            .map(packet -> (InterledgerRejectPacket) packet)
            .ifPresent(ilpRejectPacket -> {
              if (ilpRejectPacket.getCode() == InterledgerErrorCode.F08_AMOUNT_TOO_LARGE) {
                this.maxPacketAmountTracker.reduceMaxPacketAmount(ilpRejectPacket, streamPacketRequest.sourceAmount());
              } else if (streamPacketReject.isAuthentic()) { // <--  Only for any authentic packets.
                this.maxPacketAmountTracker.adjustPathCapacity(streamPacketRequest.sourceAmount());
              }
            });
        }
      );
  }
}
