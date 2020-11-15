package org.interledger.stream.sender.good;

import org.interledger.core.InterledgerErrorCode;

import com.google.common.primitives.UnsignedLong;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

// TODO: Revisit Javdoc

/**
 * Defines how to track a payment while considering the amount sent vs amount received, allowing room for
 * path-exchange-rate fluctuations and implementation-defined rules relating to whether or not to continue a payment.
 * This interface assumes a "SENDER_AMOUNT" perspective, meaning the payment is always tracked in the sender's units,
 * and all rules are applied from that perspective. This is only important if the sender and receiver amounts are
 * denominated in different currencies, and means that a request to send 10 units will never exceed that value in
 * sender's units. This implementation precludes the ability for sender to say something like, "keep sending value until
 * the receiver gets 10 units" because this type of operation should likely never be allowed (e.g, it gives the receiver
 * the ability to lie about exchange rates and potentially drain the sender's account). Instead, the required-FX-rate
 * can be adjusted to theoretically enable this "receiver-mode" scenario, but in general this should not be enabled).
 */
public interface PaymentTracker {

  /**
   * The original amount of total units to send as part of a Stream payment.
   *
   * @return An {@link UnsignedLong} containing the original amount to send.
   */
  UnsignedLong getOriginalAmount();

  /**
   * The current number of units that are still outstanding from {@link #getOriginalAmount()}. This value will reduce as
   * more packetized Stream payments are delivered until there is no more left to send.
   *
   * @return An {@link UnsignedLong} that indicates the number of unit left to send.
   */
  UnsignedLong getOriginalAmountLeft();

  /**
   * The current number of the Stream sender's units that have been delivered to the Stream receiver.
   *
   * @return An {@link UnsignedLong} that indicates the number of unit already sent.
   */
  UnsignedLong getDeliveredAmountInSenderUnits();

  /**
   * The current number of the Stream receiver's units that have been delivered to the Stream receiver.
   *
   * @return An {@link UnsignedLong} that indicates the number of unit already sent.
   */
  UnsignedLong getDeliveredAmountInReceiverUnits();

  // Synchronized because we need to ensure that two threads don't get into the update portion of amountLeftToSend
  // at the same time, but on accident. E.g., if two threads enter here trying to auth 1 unit, but amountLeftToSend is
  // 1, then both threads may succeed if they each read 1 unit.
  boolean auth(final UnsignedLong prepareAmount);

  /**
   * Rollback a packetized payment by properly adding back the values in {@code prepareAmounts} from any internal
   * tracking values.
   *
   * @param prepareAmount An {@link UnsignedLong} that contains the amount that was authorized.
   */
  void rollback(UnsignedLong prepareAmount);

  /**
   * Finalize and commit any tracking values for a packetized payment using the values in {@code prepareAmounts}.
   *
   * @param prepareAmount   An {@link UnsignedLong} that contains the amount that was authorized.
   * @param deliveredAmount A {@link UnsignedLong} that contains the amount delivered which needs to be committed.
   */
  void commit(UnsignedLong prepareAmount, UnsignedLong deliveredAmount);

  /**
   * Determine if there is more value to send for a given Stream Payment. This value is not related to the amount
   * "in-flight" because this payment tracker does not distinguish between a packet merely being auth'd (i.e., reserved)
   * and a packet being on the wire. In other words, this function will return {@code false} if there is no more value
   * available, but this doesn't mean that any particular amount of value has been sent out on a link or
   * fulfilled/rejected.
   *
   * @return {@code true} if there is still more value to send; {@code false}.
   */
  boolean moreToSend();

  boolean isPaymentComplete();

  AtomicReference<Instant> getLastFulfillTime();

  /**
   * Determine if the full value of the Stream payment was successfully delivered to the receiver.
   *
   * @return {@code true} if payment was successful; {@code false}.
   */
  default boolean successful() {
    return !moreToSend();
  }

  /**
   * Determines if the rate of reject packets that count towrds the fail-fast threshold is sufficient enough to consider
   * if this payment is "failing."
   *
   * @return {@code true} if the payment is failing; {@code false} otherwise.
   */
  boolean isPaymentFailing();

  int getNumFulfilledPackets();

  int getNumRejectedPackets();

  boolean shouldToFailImmediately(InterledgerErrorCode errorCode);
}
