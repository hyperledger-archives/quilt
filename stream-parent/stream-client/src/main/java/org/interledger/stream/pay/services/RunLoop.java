package org.interledger.stream.pay.services;

import org.interledger.link.Link;
import org.interledger.stream.pay.filters.StreamPacketFilter;
import org.interledger.stream.pay.filters.chain.DefaultStreamPacketFilterChain;
import org.interledger.stream.pay.filters.chain.StreamPacketFilterChain;
import org.interledger.stream.pay.model.ModifiableStreamPacketRequest;
import org.interledger.stream.pay.model.Quote;
import org.interledger.stream.pay.model.QuoteRequest;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.model.StreamPacketReply;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

// Scope/Purpose: Execute a Stream payment; emit events. For quoting, use a different stack of filters. For payment use another set of filters.
// TODO: Finish Javadoc!
class RunLoop {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final Link<?> link;
  private final List<StreamPacketFilter> streamPacketFilters;
  private ExecutorService executorService;

  // TODO: Consider replacing this with a timeout value?
  //private final QuoteRequest sendMoneyRequest;
//  private final Supplier<PreparePacketComponents> preparePacketComponentsSupplier;
//  private final Consumer<PreparePacketComponents> sendNextPacketConsumer;

  //private final ExecutorService perPacketExecutorService;
  //private final List<CompletableFuture<Void>> futures;

  private Instant paymentStartTime;

  public RunLoop(
      Link<?> link,
      final List<StreamPacketFilter> streamPacketFilters
      //final SendMoneyRequest sendMoneyRequest
      //final Supplier<PreparePacketComponents> preparePacketComponentsSupplier,
      //final Consumer<PreparePacketComponents> sendNextPacketConsumer
  ) {
    this.link = link;
    this.streamPacketFilters = Objects.requireNonNull(streamPacketFilters);
    //this.sendMoneyRequest = Objects.requireNonNull(sendMoneyRequest);
    //this.preparePacketComponentsSupplier = Objects.requireNonNull(preparePacketComponentsSupplier);
    //this.sendNextPacketConsumer = Objects.requireNonNull(sendNextPacketConsumer);

    //this.futures = Lists.newArrayList();
    this.executorService = Executors.newFixedThreadPool(5);
  }


  // TODO: Interface
  // TODO: use correct executor
  @VisibleForTesting
  public Quote start(QuoteRequest quoteRequest) {
    Objects.requireNonNull(quoteRequest);

    final ModifiableStreamPacketRequest streamPacketRequest = this.constructStreamPacketRequest(quoteRequest);
    final AtomicReference<SendState> currentSendState = new AtomicReference<>(SendState.Ready);

    final Supplier<StreamPacketReply> s = () -> {
      // On every iteration, execute a new filter-chain with the same filters+link. This ensure that the filters can
      // hold states, but the filter chain itself won't execute filters more than once.
      final StreamPacketFilterChain filterChain = new DefaultStreamPacketFilterChain(this.streamPacketFilters, link);
      return filterChain.doFilter(streamPacketRequest);
    };

    // Execute the run-loop.
    while (true) {
      // TODO: Assemble a runnable here so that it doesn't run unless
      // Limit this to 5 or 10 threads?
      CompletableFuture.supplyAsync(s, executorService)
          .handle((streamReply1, throwable) -> {
            if (throwable != null) {
              logger.error(throwable.getMessage(), throwable);
              return throwable;
            } else {
              this.transitionSendState(currentSendState, streamReply1.sendState());
              return streamReply1;
            }
          });

      if (currentSendState.get() == SendState.Ready) {
        try {
          // TODO: How long?
          Thread.sleep(50);
        } catch (InterruptedException e) {
          logger.error(e.getMessage(), e);
        }
      } else if (currentSendState.get() == SendState.End || currentSendState.get().isPaymentError()) {
        //TODO: Use the pending requests service to wait for any pending requests.
        //        await Promise.all(controllers.get(PendingRequestTracker).getPendingRequests())
        //throw new StreamPayerException("TODO", currentSendState.get());
        break;
      } else if (currentSendState.get() == SendState.Wait) {
        try {
          // TODO: How long?
          Thread.sleep(50);
        } catch (InterruptedException e) {
          logger.error(e.getMessage(), e);
        }
      } else {
        throw new RuntimeException(String.format("Encountered unhandled sendState: %s", currentSendState.get()));
      }
    }

    // TODO: Aggregate the payment info and return a Quote
    return Quote.builder()
        //.quoteRequest(quoteRequest)
        .build();
  }

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

  private ModifiableStreamPacketRequest constructStreamPacketRequest(QuoteRequest quoteRequest) {
    Objects.requireNonNull(quoteRequest);

    return ModifiableStreamPacketRequest.create().setSourceAmount(quoteRequest.amountToSend());
  }
}
