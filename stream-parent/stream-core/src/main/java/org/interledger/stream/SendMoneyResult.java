package org.interledger.stream;

import org.immutables.value.Value;
import org.interledger.core.Immutable;

import com.google.common.primitives.UnsignedLong;

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
   * The actual amount, in the senders units, that was delivered to the receiver.
   *
   * @return An {@link UnsignedLong} representing the amount delivered.
   */
  UnsignedLong amountDelivered();

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
   * Compare the amount delivered to the original amount to see if they match.
   *
   * @return true if delivered matches original.
   */
  @Value.Derived
  default boolean successfulPayment() {
    return amountDelivered().equals(originalAmount());
  }
}
