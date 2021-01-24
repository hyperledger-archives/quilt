package org.interledger.stream.pay;

import org.interledger.link.Link;
import org.interledger.stream.crypto.StreamPacketEncryptionService;
import org.interledger.stream.pay.exceptions.StreamPayerException;
import org.interledger.stream.pay.filters.StreamPacketFilter;
import org.interledger.stream.pay.filters.chain.DefaultStreamPacketFilterChain;
import org.interledger.stream.pay.filters.chain.StreamPacketFilterChain;
import org.interledger.stream.pay.model.ModifiableStreamPacketRequest;
import org.interledger.stream.pay.model.PaymentReceipt;
import org.interledger.stream.pay.model.PaymentStatistics;
import org.interledger.stream.pay.model.Quote;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.trackers.PaymentSharedStateTracker;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Executes a loop of packet-send attempts, based upon various information in any associated trackers found in {@link
 * org.interledger.stream.pay.trackers}.
 */
class RunLoop extends AbstractPayWrapper {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final int runLoopWaitTimeMs;
  private final Link<?> link;
  private final List<StreamPacketFilter> streamPacketFilters;
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
    this(link, streamPacketFilters, streamPacketEncryptionService, paymentSharedStateTracker, 20);
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
    super(streamPacketEncryptionService);
    this.link = Objects.requireNonNull(link);
    this.streamPacketFilters = Objects.requireNonNull(streamPacketFilters);
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

    // Execute the run-loop. Each iteration of the run-loop sends 1 ILP packet on every run. During the run, the
    // "next state" can be set by the filterChain.
    StreamPayerException streamPayerException = null;
    while (true) {
      // Construct a new FilterChain on every call so that the filters can be engaged properly and in-order.
      final StreamPacketFilterChain filterChain = this.constructNewFilterChain();

      // This call captures the nextSendState for all filters (thanks to the filterChain logic). Not done in a
      // separate thread on purposes so that the decision to send more in a different thread is always done in a
      // consistent fashion without having to worry about concurrency.
      final ModifiableStreamPacketRequest streamPacketRequest = ModifiableStreamPacketRequest.create();
      SendState nextSendState;

      try {
        nextSendState = filterChain.nextState(streamPacketRequest);
      } catch (StreamPayerException e) {
        logger.error(e.getMessage(), e);
        nextSendState = e.getSendState();
        streamPayerException = e;
      }

      if (nextSendState == SendState.Ready) {
        // This call processes doFilter for all filters (thanks to the filterChain logic). No exception will ever be
        // thrown from the chain. Instead, any exceptions will be encapsulated inside of the filterChain response.
        // For now, these responses are ignored because this code doesn't do anything with the response. However, in
        // the future, the response might be logged or emitted to a tracker of some kind. Swallowing the response here
        // is acceptable because filterChain.doFilter operates on the shared PaymentState tracker, and subsequent
        // invocations of the run-loop will react properly from there.
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
            this.closeConnection(quote.streamConnection(), SendState.getCorrespondingErrorCode(nextSendState));
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

    // Return the PaymentReceipt
    final StreamPayerException finalStreamPayerException = streamPayerException;
    return CompletableFuture.supplyAsync(() -> PaymentReceipt.builder()
      .originalQuote(quote)
      .amountDeliveredInDestinationUnits(
        paymentSharedStateTracker.getAmountTracker().getAmountDeliveredInDestinationUnits()
      )
      .amountSentInSendersUnits(paymentSharedStateTracker.getAmountTracker().getAmountSentInSourceUnits())
      .amountLeftToSendInSendersUnits(
        quote.paymentOptions().amountToSend().movePointRight(quote.sourceAccount().denomination().get().assetScale())
          .setScale(0, RoundingMode.HALF_EVEN)
          .toBigIntegerExact().subtract(paymentSharedStateTracker.getAmountTracker().getAmountSentInSourceUnits())
      )
      .paymentStatistics(PaymentStatistics.builder()
        .numFulfilledPackets(paymentSharedStateTracker.getStatisticsTracker().getNumFulfills())
        .numRejectPackets(paymentSharedStateTracker.getStatisticsTracker().getNumRejects())
        .paymentDuration(Duration.between(
          paymentSharedStateTracker.getStatisticsTracker().getPaymentStartInstant(), Instant.now()
        ))
        .lowerBoundExchangeRate(paymentSharedStateTracker.getExchangeRateTracker().getLowerBoundRate())
        .upperBoundExchangeRate(paymentSharedStateTracker.getExchangeRateTracker().getUpperBoundRate())
        .build())
      .paymentError(Optional.ofNullable(finalStreamPayerException))
      .build()
    );
  }

  /**
   * Construct a new filter chain (this method exists for testing purposes).
   *
   * @return A newly constructed instance of {@link DefaultStreamPacketFilterChain}.
   */
  @VisibleForTesting
  StreamPacketFilterChain constructNewFilterChain() {
    return new DefaultStreamPacketFilterChain(
      this.streamPacketFilters, link, getStreamEncryptionService(), paymentSharedStateTracker
    );
  }

  @Override
  protected Link<?> getLink() {
    return this.link;
  }
}
