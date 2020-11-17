package org.interledger.stream.pay.filters;

import java.util.Objects;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.stream.frames.ErrorCodes;
import org.interledger.stream.pay.filters.chain.StreamPacketFilterChain;
import org.interledger.stream.pay.model.ModifiableStreamPacketRequest;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.model.StreamPacketReply;
import org.interledger.stream.pay.model.StreamPacketRequest;
import org.interledger.stream.pay.trackers.MaxPacketAmountTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Determines how the maximum packet amount is known or discovered.
 */
public class MaxPacketAmountFilter implements StreamPacketFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(MaxPacketAmountFilter.class);

  private final MaxPacketAmountTracker maxPacketAmountTracker;

  public MaxPacketAmountFilter(final MaxPacketAmountTracker maxPacketAmountTracker) {
    this.maxPacketAmountTracker = Objects.requireNonNull(maxPacketAmountTracker);
  }


  @Override
  public SendState nextState(final ModifiableStreamPacketRequest streamPacketRequest) {
    Objects.requireNonNull(streamPacketRequest);

    // Stop sending if no capacity is available.
    if (this.maxPacketAmountTracker.isNoCapacityAvailable()) {
      streamPacketRequest.setStreamErrorCodeForConnectionClose(ErrorCodes.NoError);
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

    return filterChain.doFilter(streamPacketRequest).handleAndReturn(
      streamPacketFulfill -> {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("streamPacketFulfill={}", streamPacketFulfill);
        }
        if (streamPacketFulfill.isAuthentic()) {
          this.maxPacketAmountTracker.adjustPathCapacity(streamPacketRequest.sourceAmount());
        }
      },
      streamPacketReject -> {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("streamPacketReject={}", streamPacketReject);
        }

        // The rejectPacket should be here always, but just in case.
        streamPacketReject.interledgerResponsePacket()
          .filter(packet -> InterledgerRejectPacket.class.isAssignableFrom(packet.getClass()))
          .map(packet -> (InterledgerRejectPacket) packet)
          .ifPresent(ilpRejectPacket -> {
            final boolean isF08 = ilpRejectPacket.getCode() == InterledgerErrorCode.F08_AMOUNT_TOO_LARGE;
            if (isF08) {
              this.maxPacketAmountTracker.reduceMaxPacketAmount(ilpRejectPacket, streamPacketRequest.sourceAmount());
            }
          });
      }
    );
  }
}
