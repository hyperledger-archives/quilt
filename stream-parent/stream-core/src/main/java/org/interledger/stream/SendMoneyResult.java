package org.interledger.stream;

import org.interledger.core.Immutable;

import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value;

import java.time.Duration;

/**
 * The result of a request to send money using the STREAM protocol.
 */
@Immutable
public interface SendMoneyResult {

  static SendMoneyResultBuilder builder() {
    return new SendMoneyResultBuilder();
  }

  /**
   * The original amount that was requested to be sent.
   *
   * @return An {@link UnsignedLong} representing the original amount to be sent in a given STREAM payment.
   */
  UnsignedLong originalAmount();

  /**
   * The actual amount, in the receivers units, that was delivered to the receiver. Any currency conversion and/or
   * connector fees may cause this to be different than the amount sent.
   *
   * @return An {@link UnsignedLong} representing the amount delivered.
   */
  UnsignedLong amountDelivered();

  /**
   * The actual amount, in the senders units, that was sent to the receiver. In the case, of a timeout or rejected
   * packets this amount may be less than the requested amount to be sent.
   *
   * @return An {@link UnsignedLong} representing the amount sent.
   */
  UnsignedLong amountSent();

  /**
   * The actual amount, in the senders units, that is still left to send. If the payment was successful, this amount
   * will be 0. If there was an issue sending payment (repeated rejections or timeouts), this will be the amount that
   * could not be sent or which may have been sent but we failed to receive the fulfillment packet (network issue).
   *
   * @return An {@link UnsignedLong} representing the amount left to send.
   */
  UnsignedLong amountLeftToSend();

  /**
   * Metadata representing the number of fulfilled packets in a request to send money. Note that STREAM implementations
   * MUST close the connection once either endpoint has sent 2^31 packets.
   *
   * @return An integer representing the number of fulfilled packets in a STREAM payment.
   */
  int numFulfilledPackets();

  /**
   * Metadata representing the number of rejected packets in a request to send money. Note that STREAM implementations
   * MUST close the connection once either endpoint has sent 2^31 packets.
   *
   * @return An integer representing the number of rejected packets in a STREAM payment.
   */
  int numRejectPackets();

  /**
   * Metadata representing the total number of packets (fulfilled or rejected) sent over a given STREAM connection.
   *
   * @return An int representing the total number of packets sent over a given STREAM connection.
   */
  default int totalPackets() {
    return numFulfilledPackets() + numRejectPackets();
  }

  /**
   * Metadata representing the total time the STREAM payment took to complete or fail.
   *
   * @return A {@link Duration} representing the total time a STREAM payment took to complete or fail.
   */
  Duration sendMoneyDuration();

  /**
   * Indicates if the payment was completed successfully.
   *
   * @return true if payment was successful
   */
  boolean successfulPayment();

}
