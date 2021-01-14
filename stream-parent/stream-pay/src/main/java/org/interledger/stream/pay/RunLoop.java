package org.interledger.stream.pay;

import org.interledger.link.Link;
import org.interledger.stream.StreamPacketUtils;
import org.interledger.stream.connection.StreamConnection;
import org.interledger.stream.crypto.StreamPacketEncryptionService;
import org.interledger.stream.frames.ConnectionCloseFrame;
import org.interledger.stream.frames.StreamCloseFrame;
import org.interledger.stream.pay.filters.StreamPacketFilter;
import org.interledger.stream.pay.filters.chain.DefaultStreamPacketFilterChain;
import org.interledger.stream.pay.filters.chain.StreamPacketFilterChain;
import org.interledger.stream.pay.model.ModifiableStreamPacketRequest;
import org.interledger.stream.pay.model.PaymentReceipt;
import org.interledger.stream.pay.model.Quote;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.trackers.PaymentSharedStateTracker;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Executes a loop of packet-send attempts, based upon various information in any associated trackers found in {@link
 * org.interledger.stream.pay.trackers}.
 */
class RunLoop {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final int runLoopWaitTimeMs;
  private final Link<?> link;
  private final List<StreamPacketFilter> streamPacketFilters;
  private final StreamPacketEncryptionService streamPacketEncryptionService;
  private final PaymentSharedStateTracker paymentSharedStateTracker;

  private final ExecutorService executorService;

  /**
   * Required-args Constructor.
   *
   * @param link                          A {@link Link}.
   * @param streamPacketFilters           A {@link List} of type {@link StreamPacketFilter}.
   * @param streamPacketEncryptionService A {@link StreamPacketEncryptionService}.
   * @param paymentSharedStateTracker     A {@link PaymentSharedStateTracker}.
   */
  public RunLoop(
    final Link<?> link,
    final List<StreamPacketFilter> streamPacketFilters,
    final StreamPacketEncryptionService streamPacketEncryptionService,
    final PaymentSharedStateTracker paymentSharedStateTracker
  ) {
    // Default # of ms to wait for each RunLoop when READY.
    this(link, streamPacketFilters, streamPacketEncryptionService, paymentSharedStateTracker, 200);
  }

  /**
   * Required-args Constructor.
   *
   * @param link                          A {@link Link}.
   * @param streamPacketFilters           A {@link List} of type {@link StreamPacketFilter}.
   * @param streamPacketEncryptionService A {@link StreamPacketEncryptionService}.
   * @param paymentSharedStateTracker     A {@link PaymentSharedStateTracker}.
   * @param runLoopWaitTimeMs             An int representing the number of milliseconds to wait after each {@link
   *                                      SendState#Ready} result.
   */
  @VisibleForTesting
  RunLoop(
    final Link<?> link,
    final List<StreamPacketFilter> streamPacketFilters,
    final StreamPacketEncryptionService streamPacketEncryptionService,
    final PaymentSharedStateTracker paymentSharedStateTracker,
    final int runLoopWaitTimeMs
  ) {
    this.link = Objects.requireNonNull(link);
    this.streamPacketFilters = Objects.requireNonNull(streamPacketFilters);
    this.streamPacketEncryptionService = Objects.requireNonNull(streamPacketEncryptionService);
    this.paymentSharedStateTracker = Objects.requireNonNull(paymentSharedStateTracker);
    this.executorService = Executors.newFixedThreadPool(5);
    this.runLoopWaitTimeMs = runLoopWaitTimeMs;
  }

  /**
   * Start the run-loop with the supplied {@code quote}.
   *
   * @param quote A {@link Quote} with information gleaned from the payment path, but without yet sending real value (in
   *              other words, the payment path probing doesn't use this RunLoop, so by the time a quote is supplied to
   *              this function, it was typically constructed by actually probing the payment path with real packets).
   *
   * @return A {@link CompletableFuture} of type {@link PaymentReceipt}.
   */
  public CompletableFuture<PaymentReceipt> start(final Quote quote) {
    Objects.requireNonNull(quote);

    // PacketFilters should be stateless so that they can be created once, and re-used across multiple run-loop
    // invocations. If state is required in a filter, then a tracker should be created that can track a value
    // throughout a payment so that everytime the filter is executed, some state can be remembered.

    // NextState is called on all filters. Then doFilter is called, at which point the NextState is remembered and
    // tracked by the runLoop.

    // Execute the run-loop. Each iteration of the run-loop sends 1 ILP packet on every run. During the run, the
    // "next state" can be set by the filterChain.
    while (true) {
      final ModifiableStreamPacketRequest streamPacketRequest = ModifiableStreamPacketRequest.create();
      // Construct a new FilterChain on every call so that the filters can be engaged properly and in-order.
      final StreamPacketFilterChain filterChain = this.constructNewFilterChain();
      // This call captures the nextSendState for all filters (thanks to the filterChain logic). Not done in a
      // separate thread on purposes so that the decision to send more in a different thread is always done in a
      // consistent fashion without having to worry about concurrency.
      final SendState nextSendState = filterChain.nextState(streamPacketRequest);
      if (nextSendState == SendState.Ready) {
        // This call processes doFilter for all filters (thanks to the filterChain logic).
        CompletableFuture.runAsync(() -> filterChain.doFilter(streamPacketRequest), executorService);

        // Wait a bit before running the run-loop to give things a chance.
        try {
          if (logger.isDebugEnabled()) {
            logger.debug("Waiting {}ms in response to SendState.Ready...", runLoopWaitTimeMs);
          }
          this.sleep(this.runLoopWaitTimeMs);
        } catch (InterruptedException e) {
          logger.error(e.getMessage(), e);
        }
      } else if (nextSendState == SendState.End || nextSendState.isPaymentError()) {
        // Wait for any requests to stop.
        try {
          logger.info("Ending Payment with sendState={}", nextSendState);
          if (shouldCloseConnection(nextSendState)) {
            this.closeConnection(quote.streamConnection());
          }
          executorService.shutdown();
          // This will wait for any pending requests, up to 10 seconds at least. This means we don't need the
          // pendingRequest service.
          if (executorService.awaitTermination(10, TimeUnit.SECONDS)) {
            logger.info("Pay Executor shutdown successfully.");
          } else {
            logger.warn("Timeout elapsed before Pay Executor could shutdown successfully.");
          }
        } catch (InterruptedException e) {
          logger.error(e.getMessage(), e);
        }
        break; // <-- Exit the run-loop (no more packets)
      } else if (nextSendState == SendState.Wait) {
        try {
          if (logger.isDebugEnabled()) {
            logger.debug("Waiting 50ms in response to SendState.Wait...");
          }
          this.sleep(50);
        } catch (InterruptedException e) {
          logger.error(e.getMessage(), e);
        }
        // continue; <-- Do the run-loop one more time.
      } else {
        throw new RuntimeException(String.format("Encountered unhandled sendState: %s", nextSendState));
      }
    }

    return CompletableFuture.supplyAsync(() -> PaymentReceipt.builder()
      .amountDeliveredInDestinationUnits(
        this.paymentSharedStateTracker.getAmountTracker().getAmountDeliveredInDestinationUnits()
      )
      .amountSentInSendersUnits(
        this.paymentSharedStateTracker.getAmountTracker().getAmountSentInSourceUnits()
      )
      .originalQuote(quote)
      .build());
  }

  /**
   * Construct a new filter chain (this method exists for testing purposes).
   *
   * @return A newly constructed instance of {@link DefaultStreamPacketFilterChain}.
   */
  @VisibleForTesting
  StreamPacketFilterChain constructNewFilterChain() {
    return new DefaultStreamPacketFilterChain(
      this.streamPacketFilters, link, streamPacketEncryptionService, paymentSharedStateTracker
    );
  }

  /**
   * Close the Stream Connection by sending a final packet with both a {@link ConnectionCloseFrame} and a {@link
   * StreamCloseFrame}.
   *
   * @param streamConnection A {@link StreamConnection} to close.
   */
  @VisibleForTesting
  protected void closeConnection(StreamConnection streamConnection) {
    try {
      link.sendPacket(
        StreamPacketUtils.constructPacketToCloseStream(streamConnection, this.streamPacketEncryptionService));
    } catch (Exception e) {
      logger.error("Unable to close STREAM Connection: " + e.getMessage(), e);
      // swallow this error because the sender can still complete even though it couldn't get something to the receiver.
    }
  }

  /**
   * Based upon the supplied {@code sendState}, determines if a Connection Close frame should be sent to the
   * destination.
   *
   * @return {@code true} if the connection should be closed; {@code false} otherwise.
   */
  @VisibleForTesting
  protected boolean shouldCloseConnection(final SendState sendState) {
    Objects.requireNonNull(sendState);

    if (sendState.isPaymentError()) {
      return sendState != SendState.ClosedByRecipient; // <-- Connection already closed, so don't try to close it again.
    } else {
      return sendState == SendState.End;
    }
  }

  @VisibleForTesting
  protected void sleep(int sleepTimeMs) throws InterruptedException {
    Thread.sleep(sleepTimeMs);
  }
}
