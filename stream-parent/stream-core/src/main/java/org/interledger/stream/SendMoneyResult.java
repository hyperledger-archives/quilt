package org.interledger.stream;

import org.interledger.core.Immutable;

import com.google.common.primitives.UnsignedLong;

import java.time.Duration;

@Immutable
public interface SendMoneyResult {

  static SendMoneyResultBuilder builder() {
    return new SendMoneyResultBuilder();
  }

  UnsignedLong originalAmount();

  UnsignedLong amountDelivered();

  // Implementations MUST close the connection once either endpoint has sent 2^31 packets.
  int numFulfilledPackets();

  int numRejectPackets();

  default int totalPackets() {
    return numFulfilledPackets() + numRejectPackets();
  }

  Duration sendMoneyDuration();
}
