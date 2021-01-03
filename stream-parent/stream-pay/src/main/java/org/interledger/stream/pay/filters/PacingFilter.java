package org.interledger.stream.pay.filters;

import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.stream.pay.filters.chain.StreamPacketFilterChain;
import org.interledger.stream.pay.model.ModifiableStreamPacketRequest;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.model.StreamPacketReply;
import org.interledger.stream.pay.model.StreamPacketRequest;
import org.interledger.stream.pay.trackers.PacingTracker;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Handles any failures on the stream, and cancels a payment if no more money is fulfilled.
 */
public class PacingFilter implements StreamPacketFilter {

  // Static because these filters will be constructed a lot.
  private static final Logger LOGGER = LoggerFactory.getLogger(PacingFilter.class);

  private final PacingTracker pacingTracker;

  public PacingFilter(final PacingTracker pacingTracker) {
    this.pacingTracker = Objects.requireNonNull(pacingTracker);
  }

  @Override
  public SendState nextState(final ModifiableStreamPacketRequest streamPacketRequest) {
    Objects.requireNonNull(streamPacketRequest);

    boolean exceedsMaxInFlight = pacingTracker.getNumberInFlight() + 1 > PacingTracker.MAX_INFLIGHT_PACKETS;
    if (exceedsMaxInFlight) {
      return SendState.Wait;
    }

    if (pacingTracker.getNextPacketSendTime().isAfter(Instant.now())) {
      return SendState.Wait;
    }

    return SendState.Ready;
  }

  @Override
  public StreamPacketReply doFilter(
    final StreamPacketRequest streamRequest, final StreamPacketFilterChain filterChain
  ) {
    Objects.requireNonNull(streamRequest);
    Objects.requireNonNull(filterChain);

    final Instant sentTime = Instant.now();
    this.pacingTracker.setLastPacketSentTime(sentTime);
    this.pacingTracker.incrementNumPacketsInFlight();

    final StreamPacketReply streamPacketReply = filterChain.doFilter(streamRequest);

    this.pacingTracker.decrementNumPacketsInFlight();

    // Only update the RTT if we know the request got to the recipient
    if (streamPacketReply.isAuthentic()) {
      final long roundTripTime = Math.max(
        Instant.now().minus(sentTime.toEpochMilli(), ChronoUnit.MILLIS).toEpochMilli(), 0
      );
      this.pacingTracker.updateAverageRoundTripTime((int) roundTripTime);
    }

    // If we encounter a temporary error that's not related to liquidity, exponentially backoff the rate of
    // packet sending
    if (this.hasT04RejectCode(streamPacketReply)) {
      // Fractional rates are fine
      int reducedRate = Math.max(
        pacingTracker.MIN_PACKETS_PER_SECOND,
        this.pacingTracker.getPacketsPerSecond() / 2 // Fractional rates are fine
      );
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
          "Handling {}. backing off to {} packets / second",
          InterledgerErrorCode.T04_INSUFFICIENT_LIQUIDITY, reducedRate
        );
      }
      this.pacingTracker.setPacketsPerSecond(reducedRate);
    } else if (streamPacketReply.isAuthentic()) {
      // If the packet got through, additive increase of sending rate, up to some maximum
      this.pacingTracker.setPacketsPerSecond(
        Math.min(PacingTracker.MAX_PACKETS_PER_SECOND, pacingTracker.getPacketsPerSecond() + 1)
      );
    }

    return streamPacketReply;
  }

  /**
   * Helper method (for mocking) to determine if a Stream Packet has a T04 error code.
   *
   * @param streamPacketReply A {@link StreamPacketReply}.
   *
   * @return {@code true} if the reply has a T04 reject code; {@code false} otherwise.
   */
  @VisibleForTesting
  boolean hasT04RejectCode(final StreamPacketReply streamPacketReply) {
    Objects.requireNonNull(streamPacketReply);

    return streamPacketReply.interledgerResponsePacket()
      .filter(packet -> InterledgerRejectPacket.class.isAssignableFrom(packet.getClass()))
      .map(packet -> (InterledgerRejectPacket) packet)
      .filter(packet -> packet.getCode() == InterledgerErrorCode.T04_INSUFFICIENT_LIQUIDITY).isPresent();
  }

}
