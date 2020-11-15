package org.interledger.stream.pay.filters;

import static org.interledger.stream.utils.StreamPacketUtils.DEFAULT_STREAM_ID;

import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedLong;
import java.util.Objects;
import org.interledger.core.InterledgerAddress;
import org.interledger.stream.frames.ConnectionCloseFrame;
import org.interledger.stream.frames.ConnectionDataMaxFrame;
import org.interledger.stream.frames.ConnectionMaxStreamIdFrame;
import org.interledger.stream.frames.ConnectionNewAddressFrame;
import org.interledger.stream.frames.ErrorCodes;
import org.interledger.stream.frames.StreamMoneyMaxFrame;
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
public class AssetDetailsFilter implements StreamPacketFilter {

  // Static because these filters will be constructed a lot.
  private static final Logger logger = LoggerFactory.getLogger(AssetDetailsFilter.class.getClass());

  private PaymentSharedStateTracker paymentSharedStateTracker;

  public AssetDetailsFilter(final PaymentSharedStateTracker paymentSharedStateTracker) {
    this.paymentSharedStateTracker = Objects.requireNonNull(paymentSharedStateTracker);
  }

  @Override
  public StreamPacketReply doFilter(
    final ModifiableStreamPacketRequest streamRequest, final StreamPacketFilterChain filterChain
  ) {
    Objects.requireNonNull(streamRequest);
    Objects.requireNonNull(filterChain);

    if (this.paymentSharedStateTracker.getAssetDetailsTracker().getRemoteAssetChanged()) {
      // TODO: We send an optional close frame in the StreamPacketReply, but this is technically an error condition.
      // As an alternative, we could throw an exception, but this would change the API contract, esp something like
      // the "End" state.
      return StreamPacketReply.builder()
        .sendState(SendState.DestinationAssetConflict)
        // Inject a frame into the streamRequest so that it will be sent during close.
        .addStreamFramesForConnectionClose(
          ConnectionCloseFrame.builder()
            .errorCode(ErrorCodes.ProtocolViolation)
            .errorMessage("Destination asset changed, but this is prohibited by the IL-RFC-29.")
            .build())
        .build();
    }

    // This implementation doesn't receive packets, so only send a `ConnectionNewAddress` for backwards
    // compatibility to fetch asset details. If we already know asset details, skip this!
    if (!this.paymentSharedStateTracker.getAssetDetailsTracker().getDestinationAccountDetails()
      .denomination().isPresent()) {

      final InterledgerAddress destinationAddress = this.paymentSharedStateTracker
        .getAssetDetailsTracker().getDestinationAccountDetails().interledgerAddress();

      /**
       * Interledger.rs, Interledger4j, and `ilp-protocol-stream` < 2.5.0
       * base64 URL encode 18 random bytes for the connection token (length 24).
       *
       * But... `ilp-protocol-stream` >= 2.5.0 encrypts it using the server secret, identifying that version, which
       * is widely used in production.
       */
      final String connectionToken = destinationAddress.lastSegment();
      if (connectionToken.length() == 24) {
        /**
         * Interledger.rs rejects with an F02 if we send a `ConnectionNewAddress` frame with an invalid (e.g. empty)
         * address.
         *
         * Since both Rust & Java won't ever send any packets, we can use any address here, since it's just so they
         * reply with asset details.
         */
        streamRequest.requestFrames().add(ConnectionNewAddressFrame.builder().sourceAddress(
          this.paymentSharedStateTracker.getAssetDetailsTracker().getSourceAccountDetails().interledgerAddress()
          ).build()
        );
      } else {
        /**
         * For `ilp-protocol-stream` >= 2.5.0, send `ConnectionNewAddress` with an empty address, which will (1)
         * trigger a reply with asset details, and (2) not trigger a send loop.
         */
        streamRequest.requestFrames().add(ConnectionNewAddressFrame.builder().build());
      }
    }

    // Notify the recipient of our limits
    if (!this.paymentSharedStateTracker.getAssetDetailsTracker().getRemoteKnowsOurAccount()) {
      streamRequest.requestFrames().addAll(Lists.newArrayList(
        // Disallow incoming money (JS auto opens a new stream for this)
        StreamMoneyMaxFrame.builder()
          .streamId(DEFAULT_STREAM_ID)
          .receiveMax(UnsignedLong.ZERO)
          .totalReceived(UnsignedLong.ZERO)
          .build(),
        // Disallow incoming data
        ConnectionDataMaxFrame.builder()
          .maxOffset(UnsignedLong.ZERO)
          .build(),
        // Disallow any new streams
        ConnectionMaxStreamIdFrame.builder()
          .maxStreamId(DEFAULT_STREAM_ID)
          .build()
      ));
    }

    return filterChain.doFilter(streamRequest);
  }

}
