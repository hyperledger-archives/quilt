package org.interledger.stream.pay.filters;

import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerErrorCode.ErrorFamily;
import org.interledger.core.fluent.Percentage;
import org.interledger.stream.StreamPacketUtils;
import org.interledger.stream.frames.ErrorCodes;
import org.interledger.stream.frames.StreamFrame;
import org.interledger.stream.pay.filters.chain.StreamPacketFilterChain;
import org.interledger.stream.pay.model.ModifiableStreamPacketRequest;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.model.StreamPacketReply;
import org.interledger.stream.pay.model.StreamPacketRequest;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Handles any failures on the stream, and cancels a payment if no more money is fulfilled.
 */
public class FailureFilter implements StreamPacketFilter {

  // Static because these filters will be constructed a lot.
  private static final Logger LOGGER = LoggerFactory.getLogger(FailureFilter.class);

  /**
   * Number of milliseconds since the last Fulfill was received before the payment should fail.
   */
  private static final Duration MAX_DURATION_SINCE_LAST_FULFILL = Duration.of(10, ChronoUnit.SECONDS);

  /**
   * UNIX timestamp when the last Fulfill was received. Begins when the first fulfillable Prepare is sent.
   */
  private final AtomicReference<Optional<Instant>> lastFulfillTimeRef;

  /**
   * Should the payment end immediately due to a terminal error.
   */
  private final AtomicBoolean terminalRejectRef;

  /**
   * Was the connection or stream closed by the recipient.
   */
  private final AtomicBoolean remoteClosedRef;

  /**
   * The total number of rejection packets received on the stream, for stopping the payment if the reject:fulfill ratio
   * becomes to large.
   */
  private final AtomicInteger numRejects;

  /**
   * The total number of fulfill packets received on the stream, for stopping the payment if the reject:fulfill ratio
   * becomes to large.
   */
  private final AtomicInteger numFulfills;

  /**
   * Required-args Constructor.
   */
  public FailureFilter() {
    this.lastFulfillTimeRef = new AtomicReference<>(Optional.empty());
    this.terminalRejectRef = new AtomicBoolean();
    this.remoteClosedRef = new AtomicBoolean();

    this.numRejects = new AtomicInteger(0);
    this.numFulfills = new AtomicInteger(0);
  }

  @Override
  public SendState nextState(final ModifiableStreamPacketRequest streamPacketRequest) {
    Objects.requireNonNull(streamPacketRequest);

    if (this.terminalRejectEncountered()) {
      streamPacketRequest.setStreamErrorCodeForConnectionClose(ErrorCodes.NoError);
      return SendState.ConnectorError; // <-- Signal to close the connection.
    }

    if (this.remoteClosed()) {
      return SendState.ClosedByRecipient; // <-- Connection already closed, so no need to signal to close the connection
    }

    // Allow for rate-probe rejections.
    if ((double) getNumFulfills() / (double) getNumRejects() < 0.05) {
      LOGGER.error(
        "Too many overall rejections. Fulfill:Reject Ratio={}:{} ({})",
        getNumFulfills(), getNumRejects(),
        Percentage.of(
          new BigDecimal(getNumFulfills()).divide(new BigDecimal(getNumRejects()), 3, RoundingMode.HALF_EVEN)
        )
      );
      return SendState.End; // <-- Too many rejects in proportion to the number of fulfills.
    }

    return getLastFulfillmentTime()
      .map(lastFulfillTime -> {
        final Instant deadline = lastFulfillTime.plus(MAX_DURATION_SINCE_LAST_FULFILL);
        if (Instant.now().isAfter(deadline)) {
          LOGGER.error(
            "Ending payment because no Fulfill was received before idle deadline. lastFulfill={} deadline={}",
            lastFulfillTime, deadline
          );
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

    if (!getLastFulfillmentTime().isPresent()) {
      this.lastFulfillTimeRef.set(Optional.of(Instant.now()));
    }

    final StreamPacketReply streamPacketReply = filterChain.doFilter(streamRequest);

    // If there are frames, look for a remote close.
    this.handleRemoteClose(streamPacketReply.frames());

    // Do some things if the streamPacketReply is a fulfill or a reject.
    streamPacketReply.interledgerResponsePacket().ifPresent(ilpResponse -> ilpResponse.handle(
      fulfillPacket -> {
        this.lastFulfillTimeRef.set(Optional.of(Instant.now()));
        this.numFulfills.getAndIncrement(); // <-- Count the fulfills
      },
      rejectPacket -> {
        this.numRejects.getAndIncrement(); // <-- Count the rejections.
        // Ignore all temporary errors, F08, F99, & R01
        if (rejectPacket.getCode().getErrorFamily() == ErrorFamily.TEMPORARY ||
          rejectPacket.getCode() == InterledgerErrorCode.F08_AMOUNT_TOO_LARGE ||
          rejectPacket.getCode() == InterledgerErrorCode.F99_APPLICATION_ERROR ||
          rejectPacket.getCode() == InterledgerErrorCode.R01_INSUFFICIENT_SOURCE_AMOUNT
        ) {
          return;
        }

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

    final boolean hasCloseFrame = StreamPacketUtils.hasStreamCloseFrames(frames);
    if (hasCloseFrame) {
      StreamPacketUtils.findStreamCloseFrame(frames).ifPresent(
        streamCloseFrame -> LOGGER.error("Ending payment: receiver closed the Stream. frame={}", streamCloseFrame));

      StreamPacketUtils.findConnectionCloseFrame(frames).ifPresent(connectionCloseFrame -> LOGGER
        .error("Ending payment: receiver closed the Connection. frame={}", connectionCloseFrame));

      this.remoteClosedRef.set(true);
    }
  }

  @VisibleForTesting
  boolean terminalRejectEncountered() {
    return this.terminalRejectRef.get();
  }

  /**
   * Indicates if the remote has closed the Stream or Stream Connection being used.
   *
   * @return {@code true} if a stream or stream connection has been closed by the remote; {@code false} otherwise.
   */
  @VisibleForTesting
  boolean remoteClosed() {
    return this.remoteClosedRef.get();
  }

  /**
   * Getter for the number of Fulfill packets that have been encountered in this payment.
   *
   * @return An int.
   */
  @VisibleForTesting
  int getNumFulfills() {
    return this.numFulfills.get();
  }

  /**
   * Getter for the number of Reject packets that have been encountered in this payment.
   *
   * @return An int.
   */
  @VisibleForTesting
  int getNumRejects() {
    return this.numRejects.get();
  }

  /**
   * Getter for the last time a Fulfill packet was encountered.
   *
   * @return An optionally-present {@link Instant}.
   */
  @VisibleForTesting
  Optional<Instant> getLastFulfillmentTime() {
    return this.lastFulfillTimeRef.get();
  }
}
