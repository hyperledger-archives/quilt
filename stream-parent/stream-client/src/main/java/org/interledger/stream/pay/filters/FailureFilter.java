package org.interledger.stream.pay.filters;

import com.google.common.annotations.VisibleForTesting;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerErrorCode.ErrorFamily;
import org.interledger.stream.frames.ErrorCodes;
import org.interledger.stream.frames.StreamFrame;
import org.interledger.stream.pay.filters.chain.StreamPacketFilterChain;
import org.interledger.stream.pay.model.ModifiableStreamPacketRequest;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.model.StreamPacketReply;
import org.interledger.stream.pay.model.StreamPacketRequest;
import org.interledger.stream.utils.StreamPacketUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles any failures on the stream, and cancels a payment if no more money is fulfilled.
 */
public class FailureFilter implements StreamPacketFilter {

  // Static because these filters will be constructed a lot.
  private static final Logger LOGGER = LoggerFactory.getLogger(FailureFilter.class);

  /**
   * Number of milliseconds since the last Fulfill was received before the payment should fail
   */
  private static Duration MAX_DURATION_SINCE_LAST_FULFILL = Duration.of(10, ChronoUnit.SECONDS);

  /**
   * UNIX timestamp when the last Fulfill was received. Begins when the first fulfillable Prepare is sent
   */
  private final AtomicReference<Optional<Instant>> lastFulfillTimeRef;

  /**
   * Should the payment end immediately due to a terminal error?
   */
  private final AtomicBoolean terminalRejectRef;

  /**
   * Was the connection or stream closed by the recipient?
   */
  private final AtomicBoolean remoteClosedRef;

  //private final PaymentSharedStateTracker paymentSharedStateTracker;

  public FailureFilter(
//    final PaymentSharedStateTracker paymentSharedStateTracker
  ) {
//    this.paymentSharedStateTracker = Objects.requireNonNull(paymentSharedStateTracker);

    this.lastFulfillTimeRef = new AtomicReference<>(Optional.empty());
    this.terminalRejectRef = new AtomicBoolean();
    this.remoteClosedRef = new AtomicBoolean();
  }

  @Override
  public SendState nextState(final ModifiableStreamPacketRequest streamPacketRequest) {
    Objects.requireNonNull(streamPacketRequest);

    if (this.terminalRejectRef.get()) {
      streamPacketRequest.setStreamErrorCodeForConnectionClose(ErrorCodes.NoError);
      return SendState.ConnectorError; // <-- Signal to close the connection.
    }

    if (this.remoteClosedRef.get()) {
      return SendState.ClosedByRecipient; // <-- Connection already closed, so no need to signal to close the connection
    }

    return this.lastFulfillTimeRef.get()
      .map(lastFulfillTime -> {
        final Instant deadline = lastFulfillTime.plus(MAX_DURATION_SINCE_LAST_FULFILL);
        if (Instant.now().isAfter(deadline)) {
          LOGGER.error("Ending payment because no Fulfill was received before idle deadline. "
            + "lastFulfill={} deadline={}", lastFulfillTime, deadline);
          return SendState.IdleTimeout;
        } else {
          return SendState.Ready;
        }
      }).orElse(SendState.Ready);
  }

  @Override
  public StreamPacketReply doFilter(
    final StreamPacketRequest streamRequest, final StreamPacketFilterChain filterChain
  ) {
    Objects.requireNonNull(streamRequest);
    Objects.requireNonNull(filterChain);

    if (!lastFulfillTimeRef.get().isPresent()) {
      this.lastFulfillTimeRef.set(Optional.of(Instant.now()));
    }

    final StreamPacketReply streamPacketReply = filterChain.doFilter(streamRequest);

    // If there are frames, look for a remote close.
    this.handleRemoteClose(streamPacketReply.frames());

    // Do some things if the streamPacketReply is a fulfill or a reject.
    streamPacketReply.interledgerResponsePacket().ifPresent(ilpResponse -> ilpResponse.handle(
      fulfillPlastFulfillTimeRefacket -> this.lastFulfillTimeRef.set(Optional.of(Instant.now())),
      rejectPacket -> {
        // Ignore all temporary errors, F08, F99, & R01
        if (rejectPacket.getCode().getErrorFamily() == ErrorFamily.TEMPORARY ||
          rejectPacket.getCode() == InterledgerErrorCode.F08_AMOUNT_TOO_LARGE ||
          rejectPacket.getCode() == InterledgerErrorCode.F99_APPLICATION_ERROR ||
          rejectPacket.getCode() == InterledgerErrorCode.R01_INSUFFICIENT_SOURCE_AMOUNT
        ) {
          return;
        }

        // TODO F02, R00 (and maybe even F00) tend to be routing errors.
        //      Should it tolerate a few of these before ending the payment?
        //      Timeout error could be a routing loop though?

        // On any other error, end the payment immediately
        this.terminalRejectRef.set(true);
        LOGGER.error("Ending payment due to terminal rejectPacket={}", rejectPacket);
      }
    ));

    return streamPacketReply;
  }

  @VisibleForTesting
  protected void handleRemoteClose(Collection<StreamFrame> frames) {
    Objects.requireNonNull(frames);

    final boolean hasCloseFrame = StreamPacketUtils.hasCloseFrame(frames);
    if (hasCloseFrame) {
      StreamPacketUtils.findStreamCloseFrame(frames).ifPresent(
        streamCloseFrame -> LOGGER.error("Ending payment: receiver closed the Stream. frame={}", streamCloseFrame));

      StreamPacketUtils.findConnectionCloseFrame(frames).ifPresent(connectionCloseFrame -> LOGGER
        .error("Ending payment: receiver closed the Connection. frame={}", connectionCloseFrame));

      this.remoteClosedRef.set(true);
    }
  }
}
