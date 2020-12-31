package org.interledger.stream.pay;

import org.interledger.link.Link;
import org.interledger.stream.crypto.StreamEncryptionUtils;
import org.interledger.stream.pay.filters.StreamPacketFilter;
import org.interledger.stream.pay.filters.chain.DefaultStreamPacketFilterChain;
import org.interledger.stream.pay.filters.chain.StreamPacketFilterChain;
import org.interledger.stream.pay.model.ModifiableStreamPacketRequest;
import org.interledger.stream.pay.model.PaymentReceipt;
import org.interledger.stream.pay.model.Quote;
import org.interledger.stream.pay.model.QuoteRequest;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.model.StreamPacketRequest;
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
import java.util.concurrent.atomic.AtomicReference;

/**
 * Executes a loop of packet-send attempts, based upon various information in any associated trackers found in {@link
 * org.interledger.stream.pay.trackers}.
 */
class RunLoop {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final Link<?> link;
  private final List<StreamPacketFilter> streamPacketFilters;
  private final StreamEncryptionUtils streamEncryptionUtils;
  private final PaymentSharedStateTracker paymentSharedStateTracker;

  private final ExecutorService executorService;

  /**
   * Required-args Constructor.
   *
   * @param link                      A {@link Link}.
   * @param streamPacketFilters       A {@link List} of type {@link StreamPacketFilter}.
   * @param streamEncryptionUtils     A {@link StreamEncryptionUtils}.
   * @param paymentSharedStateTracker A {@link PaymentSharedStateTracker}.
   */
  public RunLoop(
    final Link<?> link,
    final List<StreamPacketFilter> streamPacketFilters,
    final StreamEncryptionUtils streamEncryptionUtils,
    final PaymentSharedStateTracker paymentSharedStateTracker
  ) {
    this.link = Objects.requireNonNull(link);
    this.streamPacketFilters = Objects.requireNonNull(streamPacketFilters);
    this.streamEncryptionUtils = Objects.requireNonNull(streamEncryptionUtils);
    this.paymentSharedStateTracker = Objects.requireNonNull(paymentSharedStateTracker);
    this.executorService = Executors.newFixedThreadPool(5);
  }

  /**
   * Start the run-loop with the supplied {@code quote}.
   *
   * @param quote A {@link Quote} with information gleaned from the payment path, but without yet sending real value
   *              (i.e., a quote or estimate of the payment).
   *
   * @return A {@link CompletableFuture} of type {@link PaymentReceipt}.
   */
  public CompletableFuture<PaymentReceipt> start(final Quote quote) {
    Objects.requireNonNull(quote);

    // PacketFilters should be stateless so that they can be created once, and re-used across multiple run-loop
    // invocations. If state is required in a filter, then a tracker should be created that can track a value
    // throughout a payment so that everytime the filter is executed, some state can be remembered.

    // NextState is called on all filters. Then doFilter is called, at which point the
    // NextState is remembered and tracked by the runLoop.

//    final Supplier<StreamPacketReply> doFilter = () -> {
//      // On every iteration, execute a new filter-chain with the same filters+link. This ensure that the filters can
//      // hold states, but the filter chain itself won't execute filters more than once per iteration.
//      final DefaultStreamPacketFilterChain filterchain = new DefaultStreamPacketFilterChain(this.streamPacketFilters,
//        link);
//      return filterchain.doFilter(streamPacketRequest);
//    };

//    //final DefaultStreamPacketFilterChain effFilterchain = this.filterchain;
//    // Loop through the nextState values and check to see
//    final Supplier<StreamPacketReply> nextSendState = () -> {
//      // On every iteration, execute a new filter-chain with the same filters+link. This ensure that the filters can
//      // hold states, but the filter chain itself won't execute filters more than once per iteration.
//      return effFilterchain.nextSendState(streamPacketRequest);
//    };

    // Execute the run-loop. Each iteration of the run-loop sends 1 ILP packet on every run. During the run, the
    // "next state" can be set by the
    SendState nextSendState = SendState.Ready;
    while (true) {
      // Limit this to 5 or 10 threads?

//      CompletableFuture<Object> future = CompletableFuture.supplyAsync(doFilter, executorService)
//
//        .handle((streamPacketReply, throwable) -> {
//          if (throwable != null) {
//            logger.error(throwable.getMessage(), throwable);
//            currentSendState.set(SendState.End);
//            return throwable;
//          } else {
//            this.transitionSendState(currentSendState, streamPacketReply.sendState());
//            return streamPacketReply;
//          }
//        });

      // Check NextState that was captured from previous run.
      final ModifiableStreamPacketRequest streamPacketRequest = this.constructStreamPacketRequest(quote);
      // Construct a new FilterChain on every call so that the filters can be engaged properly and in-order.
      final StreamPacketFilterChain filterChain = new DefaultStreamPacketFilterChain(
        this.streamPacketFilters, link, streamEncryptionUtils, paymentSharedStateTracker
      );
      // This call captures the nextSendState and also adjusts the streamPacketRequest.
      nextSendState = filterChain.nextState(streamPacketRequest);
      if (nextSendState == SendState.Ready) {
        CompletableFuture.runAsync(() -> filterChain.doFilter(streamPacketRequest), executorService);

        // Wait a bit before running the run-loop to give things a chance.
        try {
          if (logger.isDebugEnabled()) {
            logger.debug("Waiting 200ms in response to SendState.Ready...");
          }
          // TODO: How long?
          Thread.sleep(200);
        } catch (InterruptedException e) {
          logger.error(e.getMessage(), e);
        }
      } else if (nextSendState == SendState.End || nextSendState.isPaymentError()) {
        //TODO: Use the pending requests service to wait for any pending requests.
        //        await Promise.all(controllers.get(PendingRequestTracker).getPendingRequests())
        //throw new StreamPayerException("TODO", currentSendState.get());

        // Wait for any requests to stop.
        try {
          logger.info("Ending Payment with sendState={}", nextSendState);
          if (shouldCloseConnection(nextSendState)) {
            // TODO: Close connection
          }
          executorService.shutdown();
          if (executorService.awaitTermination(10, TimeUnit.SECONDS)) {
            logger.info("Pay Executor shutdown successfully.");
          } else {
            logger.warn("Timeout elapsed before Pay Executor could shutdown successfully.");
          }
        } catch (InterruptedException e) {
          logger.error(e.getMessage(), e);
        }
        break; // <-- Exit the runloop (no more packets)
      } else if (nextSendState == SendState.Wait) {
        try {
          // TODO: How long?
          if (logger.isDebugEnabled()) {
            logger.debug("Waiting 50ms in response to SendState.Wait...");
          }
          Thread.sleep(50);
        } catch (InterruptedException e) {
          logger.error(e.getMessage(), e);
        }
        continue; // <-- Do the runloop one more time.
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
   * <p>The 'nextState' functionality of the filter-chain constructs a fully initialized {@link StreamPacketRequest}
   * based upon the current state of all filters. However, actual sending of this packet is done in a separate thread
   * using an {@link ExecutorService}, so adjusting in-flight amounts inside of the filter-chain is not reliable without
   * synchronization of the amounts. Instead, we adjust any sensitive values here (in a non-concurrent fashion as part
   * of each run-loop) so that these values are always consistent.</p>
   *
   * <p>The primary reason that the amount adjustments performed in this method are _not_ performed in the nextState
   * calls in the filter-chain is that we want to make sure these adjustments are _always_ made, no matter how the logic
   * in the filters is adjusted. Likewise, performing this logic here allows us to revert any amounts on a failure in a
   * controlled fashion.</p>
   *
   * @param streamPacketRequest A fully assembled bucket of information that can be sued to assemble and transmit a
   *                            stream packet as part of an overall payment.
   */
//  @VisibleForTesting
//  protected CompletableFuture<Void> schedulePacketSend(
//    final StreamPacketFilterChain filterChain, final StreamPacketRequest streamPacketRequest
//  ) {
//    Objects.requireNonNull(streamPacketRequest);
//
//    ////////////////////////
//    // First, reduce any tracking amounts in the amountFilter.
//    final ComputedStreamPacketAmounts computedAmounts = this.paymentSharedStateTracker
//      .getAmountTracker().reserveTrackedAmounts(streamPacketRequest);
//    return CompletableFuture
//      .supplyAsync(() -> filterChain.doFilter(streamPacketRequest), executorService)
//      .handle((streamPacketReply, throwable) -> {
//
//        ////////////////////////
//        // If the filter-chain fails,
//
//        if (throwable != null) {
//          logger.error(throwable.getMessage(), throwable);
//          return null;
//        } else if (streamPacketReply != null) {
//          this.paymentSharedStateTracker.getAmountTracker().commitTrackedAmounts(
//            streamPacketRequest, computedAmounts, streamPacketReply
//          );
//        }
//
//        return null; // <-- For Void return type.
//      });
//  }

  /**
   * Based upon the supplied {@code sendState}, determines if a Connection Close frame should be sent to the
   * destination.
   *
   * @return
   */
  @VisibleForTesting
  protected boolean shouldCloseConnection(final SendState sendState) {
    Objects.requireNonNull(sendState);

    if (sendState.isPaymentError()) {
      if (sendState == SendState.ClosedByRecipient) {
        return false; // <-- Connection already closed, so don't try to close it again.
      } else {
        return true;
      }
    } else {
      return sendState == SendState.End;
    }
  }

//  /**
//   * @deprecated Exists only for example purposes. Quoting is not done with a run-loop. Remove this once the pay method
//   * is complete.
//   */
//  @Deprecated
//  public Quote start(QuoteRequest quoteRequest) {
//    Objects.requireNonNull(quoteRequest);
//
//    final ModifiableStreamPacketRequest streamPacketRequest = this.constructStreamPacketRequest(quoteRequest);
//    final AtomicReference<SendState> currentSendState = new AtomicReference<>(SendState.Ready);
//
//    final Supplier<StreamPacketReply> s = () -> {
//      // On every iteration, execute a new filter-chain with the same filters+link. This ensure that the filters can
//      // hold states, but the filter chain itself won't execute filters more than once.
//      final StreamPacketFilterChain filterChain = new DefaultStreamPacketFilterChain(this.streamPacketFilters, link);
//      return filterChain.doFilter(streamPacketRequest);
//    };
//
//    // Execute the run-loop.
//    while (true) {
//      // TODO: Assemble a runnable here so that it doesn't run unless
//      // Limit this to 5 or 10 threads?
//      CompletableFuture.supplyAsync(s, executorService)
//        .handle((streamReply1, throwable) -> {
//          if (throwable != null) {
//            logger.error(throwable.getMessage(), throwable);
//            return throwable;
//          } else {
//            this.transitionSendState(currentSendState, streamReply1.sendState());
//            return streamReply1;
//          }
//        });
//
//      if (currentSendState.get() == SendState.Ready) {
//        try {
//          // TODO: How long?
//          Thread.sleep(50);
//        } catch (InterruptedException e) {
//          logger.error(e.getMessage(), e);
//        }
//      } else if (currentSendState.get() == SendState.End || currentSendState.get().isPaymentError()) {
//        //TODO: Use the pending requests service to wait for any pending requests.
//        //        await Promise.all(controllers.get(PendingRequestTracker).getPendingRequests())
//        //throw new StreamPayerException("TODO", currentSendState.get());
//        break;
//      } else if (currentSendState.get() == SendState.Wait) {
//        try {
//          // TODO: How long?
//          Thread.sleep(50);
//        } catch (InterruptedException e) {
//          logger.error(e.getMessage(), e);
//        }
//      } else {
//        throw new RuntimeException(String.format("Encountered unhandled sendState: %s", currentSendState.get()));
//      }
//    }
//
//    // TODO: Aggregate the payment info and return a Quote
//    return Quote.builder()
//      //.quoteRequest(quoteRequest)
//      .build();
//  }

  private void transitionSendState(AtomicReference<SendState> currentSendState, SendState newSendState) {
    // This class's sendState can only be moved from `Ready` or `Wait`. Any other state is final.
    if (currentSendState.get() == SendState.Ready) {
      boolean result = currentSendState.compareAndSet(SendState.Ready, newSendState);
      if (!result) {
        logger.warn("Couldn't transition sendState from {} to {}", currentSendState.get(), newSendState);
      }
    } else if (currentSendState.get() == SendState.Wait) {
      boolean result = currentSendState.compareAndSet(SendState.Wait, newSendState);
      if (!result) {
        logger.warn("Couldn't transition sendState from {} to {}", currentSendState.get(), newSendState);
      }
    } else {
      // Do nothing.
    }
  }

  // TODO: Remove if unused.
  @Deprecated
  private ModifiableStreamPacketRequest constructStreamPacketRequest(QuoteRequest quoteRequest) {
    Objects.requireNonNull(quoteRequest);

    return ModifiableStreamPacketRequest.create().setSourceAmount(quoteRequest.amountToSend());
  }

  private ModifiableStreamPacketRequest constructStreamPacketRequest(final Quote quote) {
    Objects.requireNonNull(quote);
    return ModifiableStreamPacketRequest.create();
  }
}
