package org.interledger.stream.pay.filters;

import org.interledger.core.InterledgerAddress;
import org.interledger.stream.StreamPacketUtils;
import org.interledger.stream.frames.ConnectionCloseFrame;
import org.interledger.stream.frames.ConnectionDataMaxFrame;
import org.interledger.stream.frames.ConnectionMaxStreamIdFrame;
import org.interledger.stream.frames.ConnectionNewAddressFrame;
import org.interledger.stream.frames.ErrorCodes;
import org.interledger.stream.frames.StreamMoneyMaxFrame;
import org.interledger.stream.pay.exceptions.StreamPayerException;
import org.interledger.stream.pay.filters.chain.StreamPacketFilterChain;
import org.interledger.stream.pay.model.ModifiableStreamPacketRequest;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.model.StreamPacketReply;
import org.interledger.stream.pay.model.StreamPacketRequest;
import org.interledger.stream.pay.trackers.PaymentSharedStateTracker;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedLong;

import java.util.Objects;

/**
 * Determines how the maximum packet amount is known or discovered.
 */
public class AssetDetailsFilter implements StreamPacketFilter {

  private final PaymentSharedStateTracker paymentSharedStateTracker;

  /**
   * Required-args Constructor.
   *
   * @param paymentSharedStateTracker A {@link PaymentSharedStateTracker}.
   */
  public AssetDetailsFilter(final PaymentSharedStateTracker paymentSharedStateTracker) {
    this.paymentSharedStateTracker = Objects.requireNonNull(paymentSharedStateTracker);
  }

  @Override
  public SendState nextState(ModifiableStreamPacketRequest streamPacketRequest) {
    Objects.requireNonNull(streamPacketRequest);

    if (this.remoteAssetChanged()) {
      streamPacketRequest.requestFrames().add(
        ConnectionCloseFrame.builder()
          .errorCode(ErrorCodes.ProtocolViolation)
          .errorMessage("Destination asset changed, but this is prohibited by the IL-RFC-29.")
          .build()
      );
      throw new StreamPayerException(
        "Destination asset changed, but this is prohibited by the IL-RFC-29.",
        SendState.DestinationAssetConflict,
        ErrorCodes.ProtocolViolation
      );
    }

    // This implementation doesn't receive packets, so only send a `ConnectionNewAddress` for backwards
    // compatibility to fetch asset details. If we already know asset details, skip this!
    if (!this.remoteDenominationPresent()) {

      final InterledgerAddress destinationAddress = this.paymentSharedStateTracker
        .getAssetDetailsTracker().getDestinationAccountDetails().interledgerAddress();

      // Interledger.rs, Interledger4j, and `ilp-protocol-stream` < 2.5.0 base64 URL encode 18 random bytes for the
      // connection token (length 24). But... `ilp-protocol-stream` >= 2.5.0 encrypts it using the server secret,
      // identifying that version, which is widely used in production.
      final String connectionToken = destinationAddress.lastSegment();
      if (connectionToken.length() == 24) {

        // Interledger.rs rejects with an F02 if we send a `ConnectionNewAddress` frame with an invalid (e.g. empty)
        // address. Since both Rust & Java won't ever send any packets, we can use any address here, since it's just
        // so they reply with asset details.
        streamPacketRequest.requestFrames().add(ConnectionNewAddressFrame.builder().sourceAddress(
          this.paymentSharedStateTracker.getAssetDetailsTracker().getSourceAccountDetails().interledgerAddress()
          ).build()
        );
      } else {
        // For `ilp-protocol-stream` >= 2.5.0, send `ConnectionNewAddress` with an empty address, which will (1)
        // trigger a reply with asset details, and (2) not trigger a send loop.
        streamPacketRequest.requestFrames().add(ConnectionNewAddressFrame.builder().build());
      }
    }

    // Notify the recipient of our limits
    if (!remoteKnowsOurAccount()) {
      streamPacketRequest.requestFrames().addAll(Lists.newArrayList(
        // Disallow incoming money (JS auto opens a new stream for this)
        StreamMoneyMaxFrame.builder()
          .streamId(StreamPacketUtils.DEFAULT_STREAM_ID)
          .receiveMax(UnsignedLong.ZERO)
          .totalReceived(UnsignedLong.ZERO)
          .build(),
        // Disallow incoming data
        ConnectionDataMaxFrame.builder()
          .maxOffset(UnsignedLong.ZERO)
          .build(),
        // Disallow any new streams
        ConnectionMaxStreamIdFrame.builder()
          .maxStreamId(StreamPacketUtils.DEFAULT_STREAM_ID)
          .build()
      ));
    }

    return SendState.Ready;
  }

  @Override
  public StreamPacketReply doFilter(StreamPacketRequest streamRequest, StreamPacketFilterChain filterChain) {
    Objects.requireNonNull(streamRequest);
    Objects.requireNonNull(filterChain);

    StreamPacketReply streamPacketReply = filterChain.doFilter(streamRequest);

    this.paymentSharedStateTracker.getAssetDetailsTracker().handleDestinationDetails(streamPacketReply);

    return streamPacketReply;
  }

  //////////////////
  // Private Helpers
  //////////////////

  /**
   * Helper method to allow for determining if the remote asset details have changed.
   *
   * @return {@code true} the remote asset details have changed; {@code false} otherwise.
   */
  @VisibleForTesting
  boolean remoteAssetChanged() {
    return this.paymentSharedStateTracker.getAssetDetailsTracker().getRemoteAssetChanged();
  }

  /**
   * Helper method to determine if a remote denomination has been returned in the Stream.
   *
   * @return {@code true} the remote denomination has been returned by the remote; {@code false} otherwise.
   */
  @VisibleForTesting
  boolean remoteDenominationPresent() {
    return this.paymentSharedStateTracker.getAssetDetailsTracker().getDestinationAccountDetails()
      .denomination().isPresent();
  }

  /**
   * Helper method to determine if the remote knows our own account info.
   *
   * @return {@code true} if the remote knows our own account; {@code false otherwise}.
   */
  @VisibleForTesting
  boolean remoteKnowsOurAccount() {
    return this.paymentSharedStateTracker.getAssetDetailsTracker().getRemoteKnowsOurAccount();
  }
}
