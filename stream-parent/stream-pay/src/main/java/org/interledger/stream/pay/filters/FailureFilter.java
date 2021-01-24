package org.interledger.stream.pay.filters;

import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerErrorCode.ErrorFamily;
import org.interledger.stream.StreamPacketUtils;
import org.interledger.stream.frames.StreamFrame;
import org.interledger.stream.pay.exceptions.StreamPayerException;
import org.interledger.stream.pay.filters.chain.StreamPacketFilterChain;
import org.interledger.stream.pay.model.ModifiableStreamPacketRequest;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.model.StreamPacketReply;
import org.interledger.stream.pay.model.StreamPacketRequest;
import org.interledger.stream.pay.trackers.StatisticsTracker;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Handles any failures on the stream, and cancels a payment if no more money is fulfilled.
 */
public class FailureFilter implements StreamPacketFilter {

  // Static because this filter will be constructed a lot.
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

  private final StatisticsTracker statisticsTracker;

  /**
   * Required-args Constructor.
   *
   * @param statisticsTracker A {@link StatisticsTracker}.
   */
  public FailureFilter(final StatisticsTracker statisticsTracker) {
    this.statisticsTracker = Objects.requireNonNull(statisticsTracker);

    this.lastFulfillTimeRef = new AtomicReference<>(Optional.empty());
    this.terminalRejectRef = new AtomicBoolean();
    this.remoteClosedRef = new AtomicBoolean();
  }

  @Override
  public SendState nextState(final ModifiableStreamPacketRequest streamPacketRequest) {
    Objects.requireNonNull(streamPacketRequest);

    if (this.terminalRejectEncountered()) {
      // Signal to close the connection.
      throw new StreamPayerException("Terminal rejection encountered.", SendState.ConnectorError);
    }

    if (this.remoteClosed()) {
      // Connection already closed, so no need to signal to close the connection
      throw new StreamPayerException("Remote connection was closed by the receiver.", SendState.ClosedByRecipient);
    }

    // The JS implementation tracks a failure-percentage. Because this feature is not yet configurable, it is difficult
    // to use with the Lossy tests. In addition, there are certain terminal failure-types that trigger an end to the
    // payment, so this %-based cancellation seems somewhat unnecessary, especially on lossy connections where the
    // lossiness is not related to terinal errors. Therefore, we leave this code around just in case, but it is
    // otherwise unused.

    // if (FluentCompareTo.is(computeFailurePercentage()).greaterThan(Percentage.FIFTY_PERCENT)) {
    //   final String errorMessage = String.format(
    //     "Too many overall rejections. Reject:Fulfill=%s:%s (%s failure)",
    //     statisticsTracker.getNumRejects(),
    //     statisticsTracker.getNumFulfills(),
    //     computeFailurePercentage()
    //   );
    //   //Too many rejects in proportion to the number of fulfills.
    //   throw new StreamPayerException(errorMessage, SendState.End);
    // }

    return getLastFulfillmentTime()
      .map(lastFulfillTime -> {
        final Instant deadline = lastFulfillTime.plus(MAX_DURATION_SINCE_LAST_FULFILL);
        if (Instant.now().isAfter(deadline)) {
          final String errorMessage = String.format(
            "Ending payment because no Fulfill was received before idle deadline. lastFulfill=%s deadline=%s",
            lastFulfillTime,
            deadline
          );
          //Too many rejects in proportion to the number of fulfills.
          throw new StreamPayerException(errorMessage, SendState.IdleTimeout);
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
        this.statisticsTracker.incrementNumFulfills(); // <-- Count the fulfills
      },
      rejectPacket -> {
        this.statisticsTracker.incrementNumRejects(); // <-- Count the rejections.
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
   * Getter for the last time a Fulfill packet was encountered.
   *
   * @return An optionally-present {@link Instant}.
   */
  @VisibleForTesting
  protected Optional<Instant> getLastFulfillmentTime() {
    return this.lastFulfillTimeRef.get();
  }
}
