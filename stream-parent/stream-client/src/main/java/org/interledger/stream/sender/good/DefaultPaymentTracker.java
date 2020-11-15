package org.interledger.stream.sender.good;

import static org.interledger.core.InterledgerErrorCode.F08_AMOUNT_TOO_LARGE;
import static org.interledger.core.InterledgerErrorCode.F99_APPLICATION_ERROR;
import static org.interledger.core.InterledgerErrorCode.R01_INSUFFICIENT_SOURCE_AMOUNT;
import static org.interledger.core.InterledgerErrorCode.T00_INTERNAL_ERROR;
import static org.interledger.core.InterledgerErrorCode.T01_PEER_UNREACHABLE;
import static org.interledger.core.fluent.FluentCompareTo.is;

import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerErrorCode.ErrorFamily;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.UnsignedLong;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An implementation of {@link PaymentTracker} that uses a fixed amount to send, denominated in the sender's units, as
 * reflected in the `amountToDeliver`.
 */
class DefaultPaymentTracker implements PaymentTracker {

  // Minimum number of packet attempts before failing the entire payment "fast".
  private static final int FAIL_FAST_MINIMUM_PACKET_ATTEMPTS = 200;

  // The percent of failing packets that are required in order to fail a payment (after the above threshold has been
  // met).
  private static final float FAIL_FAST_MINIMUM_FAILURE_RATE = 0.99f;

  // The original amount, in sender's units, to send
  private final UnsignedLong originalAmountToSend;
  // The amount, in sender's units, left to send (i.e., the unsent amount). Note that this value may not yet be
  // "in-flight" if the packet has been auth'd but hasn't yet been sent out on a link. Only the Congestion Controller
  // tracks in-flight amounts.
  private final AtomicReference<UnsignedLong> amountLeftToSend;
  // The amount, in sender's units, that was sent (incremented in response to fullfill packets).
  private final AtomicReference<UnsignedLong> deliverAmountInSenderUnits;
  // The amount, in receiver's units, that was actually delivered to the receiver (incremented in response to fullfill
  // packets).
  private final AtomicReference<UnsignedLong> deliveredAmountInRecevierUnits;

  private final AtomicInteger numFulfilledPackets;
  private final AtomicInteger numRejectedPackets;
  private final AtomicInteger numFailFastRejects;

  private final AtomicReference<Instant> lastFulFillTime;

  /**
   * Required-args Constructor.
   *
   * @param originalAmountToSend An {@link UnsignedLong} representing the amount to send, in the sender's units.
   */
  public DefaultPaymentTracker(final UnsignedLong originalAmountToSend) {
    this.originalAmountToSend = Objects.requireNonNull(originalAmountToSend);

    this.amountLeftToSend = new AtomicReference<>(originalAmountToSend);
    this.deliverAmountInSenderUnits = new AtomicReference<>(UnsignedLong.ZERO);
    this.deliveredAmountInRecevierUnits = new AtomicReference<>(UnsignedLong.ZERO);
    this.lastFulFillTime = new AtomicReference(Instant.now());

    this.numFulfilledPackets = new AtomicInteger(0);
    this.numRejectedPackets = new AtomicInteger(0);
    this.numFailFastRejects = new AtomicInteger(0);
  }

  @Override
  public UnsignedLong getOriginalAmount() {
    return originalAmountToSend;
  }

  @Override
  public UnsignedLong getOriginalAmountLeft() {
    return amountLeftToSend.get();
  }

  @Override
  public UnsignedLong getDeliveredAmountInSenderUnits() {
    return deliverAmountInSenderUnits.get();
  }

  @Override
  public UnsignedLong getDeliveredAmountInReceiverUnits() {
    return deliveredAmountInRecevierUnits.get();
  }

  // Synchronized because we need to ensure that two threads don't get into the update portion of amountLeftToSend
  // at the same time, but on accident. E.g., if two threads enter here trying to auth 1 unit, but amountLeftToSend is
  // 1, then both threads may succeed if they each read 1 unit.
  @Override
  public synchronized boolean auth(final UnsignedLong prepareAmount) {
    Objects.requireNonNull(prepareAmount);

    if (is(deliverAmountInSenderUnits.get().plus(prepareAmount)).greaterThan(originalAmountToSend)
        || is(amountLeftToSend.get()).lessThan(prepareAmount)) {
      return false;
    } else {
      this.amountLeftToSend.getAndUpdate(amt -> amt.minus(prepareAmount));
      return true;
    }
  }

  // No need to synchronize because prepareAmounts are fixed, and we're only adding to totals.
  @Override
  public void rollback(final UnsignedLong prepareAmount) {
    Objects.requireNonNull(prepareAmount);
    this.numRejectedPackets.incrementAndGet();
    this.amountLeftToSend.getAndUpdate(sourceAmount -> sourceAmount.plus(prepareAmount));
  }

  // No need to synchronize because prepareAmounts are fixed, and commits can be eventually consistent as long as we're
  // using AtomicReferences.
  @Override
  public void commit(final UnsignedLong prepareAmount, final UnsignedLong deliveredAmount) {
    Objects.requireNonNull(prepareAmount);
    Objects.requireNonNull(deliveredAmount);
    this.deliveredAmountInRecevierUnits.getAndUpdate(currentAmount -> currentAmount.plus(deliveredAmount));
    this.deliverAmountInSenderUnits.getAndUpdate(currentAmount -> currentAmount.plus(prepareAmount));
    this.numFulfilledPackets.incrementAndGet();
    this.lastFulFillTime.set(Instant.now());
  }

  @Override
  public boolean moreToSend() {
    return is(amountLeftToSend.get()).greaterThan(UnsignedLong.ZERO);
    //return is(this.deliverAmountInSenderUnits.get()).lessThan(this.originalAmountToSend);
  }

  @Override
  public boolean isPaymentComplete(){
    return is(this.deliverAmountInSenderUnits.get()).greaterThanEqualTo(this.originalAmountToSend);
  }

  @Override
  public AtomicReference<Instant> getLastFulfillTime() {
    return lastFulFillTime;
  }

//  @Override
//  public UnsignedLong getAmountInFlight() {
//    return amountToSend.minus(amountLeftToSend.get());
//  }

  /**
   * Determines if the rate of reject packets that count towrds the fail-fast threshold is sufficient enough to consider
   * if this payment is "failing."
   *
   * @return {@code true} if the payment is failing; {@code false} otherwise.
   */
  @Override
  public boolean isPaymentFailing() {
    int totalPackets = this.numFulfilledPackets.get() + this.numRejectedPackets.get();
    return totalPackets >= FAIL_FAST_MINIMUM_PACKET_ATTEMPTS
        && (this.numFailFastRejects.get() / totalPackets) > FAIL_FAST_MINIMUM_FAILURE_RATE;
  }

  // TODO: Remove?
//  @VisibleForTesting
//  void setDeliverAmountInSenderUnits(UnsignedLong deliverAmountInSenderUnits) {
//    this.deliverAmountInSenderUnits.set(deliverAmountInSenderUnits);
//  }

  /**
   * Only F99, T00, and T01 apply to the fail-slow threshold. Other final/relative errors should immediately fail;
   * T02-T99 may be resolved with time (especially T04).
   *
   * @param errorCode
   *
   * @return
   */
  @VisibleForTesting
  boolean shouldContributeToFailFastThreshold(final InterledgerErrorCode errorCode) {
    Objects.requireNonNull(errorCode);
    return errorCode == F99_APPLICATION_ERROR || errorCode == T00_INTERNAL_ERROR || errorCode == T01_PEER_UNREACHABLE;
  }

  /**
   * All Relative and Final Error (except F99, T00, and T01) apply to the fail-fast threshold. T02-T99 may be resolved
   * with time.
   *
   * @param errorCode
   *
   * @return
   */
  // TODO: Remove?
  @Override
  public boolean shouldToFailImmediately(final InterledgerErrorCode errorCode) {
    Objects.requireNonNull(errorCode);
    final ErrorFamily family = errorCode.getErrorFamily();

    return (
        // Fail-slow errors should not terminate immediately.
        !shouldContributeToFailFastThreshold(errorCode) &&
            // Continue sending on R01 errors, which are triggered when the router receives a packet that rounds to 0.
            errorCode != R01_INSUFFICIENT_SOURCE_AMOUNT &&
            // F08 codes will not be retried, but will adjust the congestion controller, so don't fail on them.
            errorCode != F08_AMOUNT_TOO_LARGE &&
            // Temp errors either fail-slow, or don't trigger failure.
            family != ErrorFamily.TEMPORARY
    );
  }

  @Override
  public int getNumFulfilledPackets() {
    return numFulfilledPackets.get();
  }

  @Override
  public int getNumRejectedPackets() {
    return numRejectedPackets.get();
  }
}
