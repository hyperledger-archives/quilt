package org.interledger.stream.sender.good;

import static org.interledger.core.fluent.FluentCompareTo.is;
import static org.interledger.stream.sender.good.PaymentLoopState.CONNECTION_CLOSABLE;
import static org.interledger.stream.sender.good.PaymentLoopState.FAIL_FAST;
import static org.interledger.stream.sender.good.PaymentLoopState.MAX_IN_FLIGHT;
import static org.interledger.stream.sender.good.PaymentLoopState.MORE_TO_SEND;
import static org.interledger.stream.sender.good.PaymentLoopState.PAYMENT_TIMED_EXCEEDED;
import static org.interledger.stream.sender.good.PaymentLoopState.WAIT_AND_SEE;

import org.interledger.stream.good.SendMoneyRequest;
import org.interledger.stream.sender.CongestionController;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

// TODO: Finish Javadoc!
class RunLoopStateMachine {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  // Maximum time we should wait (since last fulfill) before we error out to avoid getting into an infinite loop of
  // sending packets and effectively DoSing ourselves.
  private final long MAX_TIME_SINCE_LAST_FULFILL = 30000;

  private final AtomicReference<PaymentLoopState> currentState;
  private final PaymentTracker paymentTracker;
  private final CongestionController congestionController;
  // TODO: Consider replacing this with a timeout value?
  private final SendMoneyRequest sendMoneyRequest;
  private final Supplier<PreparePacketComponents> preparePacketComponentsSupplier;
  private final Consumer<PreparePacketComponents> sendNextPacketConsumer;

  private final ExecutorService perPacketExecutorService;
  private final List<CompletableFuture<Void>> futures;

  private Instant paymentStartTime;

  public RunLoopStateMachine(
      final ExecutorService perPacketExecutorService,
      final PaymentTracker paymentTracker,
      final CongestionController congestionController,
      final SendMoneyRequest sendMoneyRequest,
      final Supplier<PreparePacketComponents> preparePacketComponentsSupplier,
      final Consumer<PreparePacketComponents> sendNextPacketConsumer
  ) {
    this.perPacketExecutorService = Objects.requireNonNull(perPacketExecutorService);
    this.paymentTracker = Objects.requireNonNull(paymentTracker);
    this.congestionController = Objects.requireNonNull(congestionController);
    this.sendMoneyRequest = Objects.requireNonNull(sendMoneyRequest);
    this.preparePacketComponentsSupplier = Objects.requireNonNull(preparePacketComponentsSupplier);
    this.sendNextPacketConsumer = Objects.requireNonNull(sendNextPacketConsumer);

    this.futures = Lists.newArrayList();
    this.currentState = new AtomicReference<>(MORE_TO_SEND);
  }


  // TODO: Interface
  // TODO: use correct executor
  @VisibleForTesting
  public void start() {
    if (isInFinalizedState()) {
      throw new IllegalStateException(String
          .format("StateMachine is in a finalized state and cannot be re-used. CurrentState=%s",
              this.getCurrentState()));
    }
    this.paymentStartTime = Instant.now();

    while (true) {
      if (logger.isDebugEnabled()) {
        logger.debug("LoopStart: {}", this.getCurrentState());
      }
      if (isPaymentTimedOut()) {
        this.nextState(PAYMENT_TIMED_EXCEEDED);
      } else if (isPaymentFailing()) {
        this.nextState(FAIL_FAST);
      } else if (isPaymentComplete()) {
        this.nextState(CONNECTION_CLOSABLE);
      } else if (isMaxInFlight()) {
        this.nextState(MAX_IN_FLIGHT);
      } else if (isMoreToSend()) {
        this.nextState(MORE_TO_SEND);
      } else {
        this.nextState(WAIT_AND_SEE);
      }
      if (logger.isDebugEnabled()) {
        logger.debug("LoopEnd: {}", this.getCurrentState());
      }
      switch (this.getCurrentState()) {
        case WAIT_AND_SEE: {
          try {
            // Wait 500ms to see what happens...
            Thread.sleep(500);
          } catch (InterruptedException e) {
            throw new RuntimeException(e.getMessage(), e);
          }
        }
        case MORE_TO_SEND: {
          this.onMoreToSend();
          break;
        }
        case FAIL_FAST: {
          this.onPaymentIsFailing();
          break;
        }
        case MAX_IN_FLIGHT: {
          this.onMaxInFlight();
          break;
        }
        case CONNECTION_CLOSABLE: {
          this.onConnectionClosable();
          return;
        }
        case PAYMENT_TIMED_EXCEEDED: {
          this.onPaymentTimedOut();
          // TODO: Error?
          return;
        }
        default: {
          logger.warn("Unhandled RunLoop State: {}", this.getCurrentState());
        }
      }
    }
  }

  private boolean isMoreToSend() {
    return !paymentTracker.successful() && paymentTracker.moreToSend();
  }

  @VisibleForTesting
  boolean isInFinalizedState() {
    return this.getCurrentState() != MORE_TO_SEND
        && this.getCurrentState() != MAX_IN_FLIGHT;
  }

  /**
   * Determines if the maximum amount of value is currently "in-flight". This can occur in two scenarios: First, when
   * the congestion controller has no more value left in its window. Second, when the packet tracker has no more value
   * to send.
   *
   * @return
   */
  private boolean isMaxInFlight() {
    if (logger.isDebugEnabled()) {
      logger.debug("isMaxInFlight");
    }

    return is(congestionController.getAmountLeftInWindow()).lessThanOrEqualTo(UnsignedLong.ZERO)
        || !this.paymentTracker.moreToSend();
  }

  private boolean isPaymentComplete() {
    if (logger.isDebugEnabled()) {
      logger.debug("isPaymentComplete");
    }
    return paymentTracker.isPaymentComplete();
  }

  private boolean isPaymentFailing() {
    if (logger.isDebugEnabled()) {
      logger.debug("isPaymentFailing");
    }
    return paymentTracker.isPaymentFailing();
  }

  private boolean isPaymentTimedOut() {
    if (logger.isDebugEnabled()) {
      logger.debug("isPaymentTimedOut");
    }
    // TODO: Also, there should be an overall timeout (e.g., 5 mins) for the entire payment.

    final boolean overallPaymentTimedOut = sendMoneyRequest.paymentTimeout()
        .map(paymentTimeOut -> is(Duration.between(Instant.now(), paymentStartTime)).greaterThan(paymentTimeOut))
        .orElse(false);

    if (overallPaymentTimedOut ||
        is(paymentTracker.getLastFulfillTime().get().until(Instant.now(), ChronoUnit.SECONDS))
            .greaterThan(MAX_TIME_SINCE_LAST_FULFILL)
    ) {
      return true; // Payment timed out.
    } else {
      return false; // Payment not timed out.
    }
  }

  // TODO: Interface
  void onPaymentTimedOut() {
    logger.error("onPaymentTimedOut");
    futures.stream()
        .filter(cf -> !cf.isCancelled() && !cf.isDone() && !cf.isCompletedExceptionally())
        .forEach(cf -> cf.cancel(true));
  }

  // TODO: Interface
  void onPaymentIsFailing() {
    logger.error("onPaymentIsFailing");
    this.nextState(CONNECTION_CLOSABLE);
    this.onConnectionClosable();
  }

  // TODO: Interface
  void onConnectionClosable() {
    if (logger.isDebugEnabled()) {
      logger.debug("onConnectionClosable");
    }
    // Wait for all pending requests to complete before closing the connection (this will wait up to the packet timeout)
    futures.stream()
        .filter(cf -> !cf.isCancelled() && !cf.isDone() && !cf.isCompletedExceptionally())
        .forEach(cf -> cf.join());

    // Try to the tell the recipient the connection is closed
    // TODO: Uncomment!
    //this.doCloseConnection();
    //sender.try_send_connection_close().await;

    // final result will be returned when the loop stops, which will happen after this function returns.
  }

//  /**
//   * Close the current STREAM connection by sending a {@link StreamCloseFrame} to the receiver.
//   *
//   * @return An {@link UnsignedLong} representing the amount delivered by this individual stream.
//   */
//  @VisibleForTesting
//  // TODO: Could this simply be a supplier passed-in?
//  void closeStream() throws StreamConnectionClosedException {
//    this.sendStreamFramesInZeroValuePacket(Lists.newArrayList(
//        StreamCloseFrame.builder()
//            .streamId(UnsignedLong.ONE)
//            .errorCode(ErrorCodes.NoError)
//            .build()
//    ));
//  }

//  private void sendStreamFramesInZeroValuePacket(final Collection<StreamFrame> streamFrames)
//      throws StreamConnectionClosedException {
//    Objects.requireNonNull(streamFrames);
//
//    if (streamFrames.size() <= 0) {
//      logger.warn("sendStreamFrames called with 0 frames");
//      return;
//    }
//
//    final StreamPacket streamPacket = StreamPacket.builder()
//        .interledgerPacketType(InterledgerPacketType.PREPARE)
//        .prepareAmount(UnsignedLong.ZERO)
//        .sequence(streamConnection.nextSequence())
//        .addAllFrames(streamFrames)
//        .build();
//
//    // Create the ILP Prepare packet using an encrypted StreamPacket as the encryptedStreamPacket payload...
//    final byte[] encryptedStreamPacket = streamEncryptionUtils
//        .toEncrypted(sendMoneyRequest.sharedSecret(), streamPacket);
//    final InterledgerCondition executionCondition;
//    executionCondition = generatedFulfillableFulfillment(sendMoneyRequest.sharedSecret(), encryptedStreamPacket)
//        .getCondition();
//
//    final InterledgerPreparePacket preparePacket = InterledgerPreparePacket.builder()
//        .destination(sendMoneyRequest.destinationAddress())
//        .amount(UnsignedLong.ZERO)
//        .executionCondition(executionCondition)
//        .expiresAt(DateUtils.now().plusSeconds(30L))
//        .data(encryptedStreamPacket)
//        .typedData(streamPacket)
//        .build();
//
//    link.sendPacket(preparePacket);
//
//    //TODO: FIXME per https://github.com/hyperledger/quilt/issues/308
//    // Mark the streamConnection object as closed if the caller supplied a ConnectionCloseFrame
////       streamFrames.stream()
////           .filter(streamFrame -> streamFrame.streamFrameType() == StreamFrameType.ConnectionClose)
////           .findAny()
////           .ifPresent($ -> {
////             streamConnection.closeConnection();
////             logger.info("STREAM Connection closed.");
////           });
//
//    // Emit a log statement if the called supplied a StreamCloseFrame
//    streamFrames.stream()
//        .filter(streamFrame -> streamFrame.streamFrameType() == StreamFrameType.StreamClose)
//        .findAny()
//        .map($ -> (StreamCloseFrame) $)
//        .ifPresent($ -> {
//          logger.info(
//              "StreamId {} Closed. Delivered: {} ({} packets fulfilled, {} packets rejected)",
//              $.streamId(), paymentTracker.getDeliveredAmountInReceiverUnits(),
//              paymentTracker.getNumFulfilledPackets(),
//              paymentTracker.getNumRejectedPackets()
//          );
//        });
//  }

  // TODO: Interface.
  public void onMaxInFlight() {
    // The 'timeToWait' is the amount of time to wait until we hit the MAX_TIME_SINCE_LAST_FULFILL. E.g., if
    // MAX_TIME_SINCE_LAST_FULFILL is 30 seconds, and the last fulfill was 20 seconds ago, then we should wait another
    // 10 seconds before waiting for another fulfill. Running the loop will either send more money, or take us back
    // to here.
    int timeToWait = (int) Math.max(0, MAX_TIME_SINCE_LAST_FULFILL - paymentTracker.getLastFulfillTime().get()
        .until(Instant.now(), ChronoUnit.MILLIS));

    try {
      // TODO: If timeToWait == 0, then just shut everything down.
      logger.info("Waiting up to {} ms for another sendPacket", timeToWait);

      final CompletableFuture<Void> nextPending = this.selectNextPending();
      if (nextPending != null) {
        nextPending.whenComplete(($, throwable) -> {
          if (throwable != null) {
            logger.error(throwable.getMessage(), throwable);
          }
        }).get(timeToWait, TimeUnit.MILLISECONDS);
      } else {
        // There are no more pending futures, so we should continue
      }
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      this.nextState(PAYMENT_TIMED_EXCEEDED);
      final String errorMessage = String.format("Too much time since last fulfill. %s", e.getMessage());
      logger.error(errorMessage, e);
    }
  }

  private CompletableFuture<Void> selectNextPending() {
    final CompletableFuture returnable = CompletableFuture.anyOf(
        futures.stream()
            .filter(future -> !future.isCompletedExceptionally())
            .filter(future -> !future.isDone())
            .filter(future -> !future.isCancelled())
            .collect(Collectors.toList())
            .toArray(new CompletableFuture[0])
    );
    return returnable;
  }

  // TODO: Interface + Javadoc.
  public void onMoreToSend() {
    if (logger.isDebugEnabled()) {
      logger.debug("SENDING 1 PACKET!");
    }

    // This reserves money to be sent but the "send" doesn't actually happen until the future is executed (the Prepare
    // packet is also late-bound so that the expiry is correct).
    final PreparePacketComponents preparePacketComponents = this.preparePacketComponentsSupplier.get();
    if (is(preparePacketComponents.prepareAmounts().amountToSend()).greaterThan(UnsignedLong.ZERO)) {
      this.futures.add(CompletableFuture
          .runAsync(
              () -> sendNextPacketConsumer.accept(preparePacketComponents), perPacketExecutorService
          )
          .whenComplete(($, throwable) -> {
            if (throwable != null) {
              logger.error(throwable.getMessage(), throwable);
            } else {
              logger.info("FUTURE COMPLETED: {}", $);
            }
          })
      );
    }
  }

  /**
   * Transitions the current-state to the {@code requestedNextState} if allowed by the state machine rules.s
   *
   * @param requestedNextState
   *
   * @return
   */
  public boolean nextState(final PaymentLoopState requestedNextState) {
    final AtomicBoolean success = new AtomicBoolean();

    currentState.getAndUpdate(currentState -> {
      if (currentState == requestedNextState) {
        success.set(true);
        return requestedNextState;
      } else if (currentState == MORE_TO_SEND || currentState == PaymentLoopState.MAX_IN_FLIGHT) {
        // MORE_TO_SEND and MAX_IN_FLIGHT may transition to any other state.
        success.set(true);
        return requestedNextState;
      } else if (currentState == PaymentLoopState.FAIL_FAST
          && requestedNextState == PaymentLoopState.CONNECTION_CLOSABLE) {
        // FAIL_FAST --> CONNECTION_CLOSABLE
        success.set(true);
        return PaymentLoopState.CONNECTION_CLOSABLE;
      } else if (currentState == PAYMENT_TIMED_EXCEEDED
          && requestedNextState == PaymentLoopState.CONNECTION_CLOSABLE) {
        // PAYMENT_TIME_EXCEEDED --> CONNECTION_CLOSABLE
        success.set(true);
        return PaymentLoopState.CONNECTION_CLOSABLE;
      } else if (currentState == PaymentLoopState.CONNECTION_CLOSABLE) {
        // CONNECTION_CLOSABLE is a final state.
        success.set(true);
        return PaymentLoopState.CONNECTION_CLOSABLE;
      } else {
        success.set(false);
        logger.warn("Returning the current state, but this should not happen. currentState={} requestedNextState={}",
            currentState, requestedNextState);
        return currentState;
      }
    });

    return success.get();
  }

  public PaymentLoopState getCurrentState() {
    return currentState.get();
  }

}
