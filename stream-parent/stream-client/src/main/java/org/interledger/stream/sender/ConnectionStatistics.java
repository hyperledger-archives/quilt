package org.interledger.stream.sender;

import org.interledger.core.Immutable;

import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value.Derived;
// TODO: Move to stream-core?
/**
 * Contains summary information about a STREAM Connection.
 */
@Immutable
public interface ConnectionStatistics {

  static ConnectionStatisticsBuilder builder() {
    return new ConnectionStatisticsBuilder();
  }

  /**
   * The number of fulfilled packets that were received over the lifetime of this connection.
   *
   * @return An int representing the number of fulfilled packets.
   */
  int numFulfilledPackets();

  /**
   * The number of rejected packets that were received over the lifetime of this connection.
   *
   * @return An int representing the number of rejected packets.
   */
  int numRejectPackets();

  /**
   * Compute the total number of packets that were fulfilled or rejected on this Connection.
   *
   * @return An int representing the total number of response packets processed.
   */
  @Derived
  default int totalPackets() {
    return numFulfilledPackets() + numRejectPackets();
  }

  /**
   * The total amount delivered to the receiver.
   *
   * @return An {@link UnsignedLong} representing the total amount delivered.
   */
  UnsignedLong amountDelivered();
}
