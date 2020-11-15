package org.interledger.stream.pay.filters;

import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.stream.frames.ConnectionCloseFrame;
import org.interledger.stream.frames.ErrorCodes;
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

  private static final Logger LOGGER = LoggerFactory.getLogger(MaxPacketAmountFilter.class.getClass());

  private final MaxPacketAmountTracker maxPacketAmountTracker;

  public MaxPacketAmountFilter(final MaxPacketAmountTracker maxPacketAmountTracker) {
    this.maxPacketAmountTracker = Objects.requireNonNull(maxPacketAmountTracker);
  }

//  /**
//   * Signal if sending should continue and iteratively compose the next packet. Any filter can choose to immediately end
//   * the entire STREAM payment with an error, or choose to wait before sending the next packet.
//   *
//   * Note: the packet may not be sent if other controllers decline, so don't apply side effects.
//   *
//   * @param modifiableStreamRequest A {@link ModifiableStreamPacketRequest} that can be mutated to eventually construct
//   *                                a {@link SendState}.
//   *
//   * @return
//   */
//  @Override
//  public SendState nextState(final ModifiableStreamPacketRequest modifiableStreamRequest) {
//    Objects.requireNonNull(modifiableStreamRequest);
//    if (this.noCapacityAvailable.get()) {
//      this.closeConnection(modifiableStreamRequest);
//      return SendState.ConnectorError;
//    }
//
//    return SendState.Ready;
//  }

  /**
   * Applies logic to an incoming Prepare packet, optionally preventing the packet from being processed by the packet
   * switching framework.
   *
   * @param streamRequest The {@link StreamPacketRequest} that this filter is operating upon.
   * @param filterChain   The {@link StreamPacketFilterChain} that this filter is operating inside of.
   *
   * @return An optionally-present ILP response packet.
   */
  @Override
  public StreamPacketReply doFilter(
      final ModifiableStreamPacketRequest streamRequest, final StreamPacketFilterChain filterChain
  ) {
    Objects.requireNonNull(streamRequest);
    Objects.requireNonNull(filterChain);

    // Stop processing if no capacity is available.
    if (this.maxPacketAmountTracker.isNoCapacityAvailable()) {
      this.closeConnection(streamRequest);
      return StreamPacketReply.builder().sendState(SendState.End).build();
    }

    return filterChain.doFilter(streamRequest).handleAndReturn(
        streamPacketFulfill -> {
          LOGGER.info("{}", streamPacketFulfill);
        },
        streamPacketReject -> {
          boolean isF08 = streamPacketReject.interledgerResponsePacket()
              .filter(packet -> InterledgerRejectPacket.class.isAssignableFrom(packet.getClass()))
              .map(packet -> (InterledgerRejectPacket) packet)
              .filter(packet -> packet.getCode() == InterledgerErrorCode.F08_AMOUNT_TOO_LARGE)
              .map($ -> true)
              .orElse(false);
          if (isF08) {
            //this.maxPacketAmountService.reduceMaxPacketAmount(streamPacketReject, streamRequest.sourceAmount());
          } else if (streamPacketReject.isAuthentic()) {
//            this.maxPacketAmountService.adjustPathCapacity(streamRequest.sourceAmount());
          }
        }
    );
  }

  void closeConnection(final ModifiableStreamPacketRequest modifiableStreamRequest) {
    Objects.requireNonNull(modifiableStreamRequest);

    // Close the connection.
    modifiableStreamRequest.requestFrames().add(
        ConnectionCloseFrame.builder()
            .errorCode(ErrorCodes.NoError)
            .build()
    );
    modifiableStreamRequest.streamConnection().sendRequest(modifiableStreamRequest.toImmutable());
  }


}
