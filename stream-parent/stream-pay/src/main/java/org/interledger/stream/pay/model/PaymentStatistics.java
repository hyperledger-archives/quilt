package org.interledger.stream.pay.model;

import org.interledger.core.fluent.Percentage;
import org.interledger.core.fluent.Ratio;

import org.immutables.value.Value.Immutable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

/**
 * Statistics for a completed payment.
 */
@Immutable
public interface PaymentStatistics {

  static ImmutablePaymentStatistics.Builder builder() {
    return ImmutablePaymentStatistics.builder();
  }

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
   * The percentage of packets that were rejected during this payment.
   *
   * @return A {@link Percentage}.
   */
  default Percentage packetFailurePercentage() {
    if (this.numRejectPackets() <= 0) {
      return Percentage.ZERO_PERCENT; // <-- Guards against divide by zero.
    } else {
      final double failurePercentDouble = ((double) this.numRejectPackets()) / ((double) this.numTotalPackets());
      final BigDecimal failurePercentage = new BigDecimal(failurePercentDouble).setScale(3, RoundingMode.HALF_EVEN);
      return Percentage.of(failurePercentage);
    }
  }

  /**
   * Metadata representing the total number of packets (fulfilled or rejected) sent over a given STREAM connection.
   *
   * @return An int representing the total number of packets sent over a given STREAM connection.
   */
  default int numTotalPackets() {
    return numFulfilledPackets() + numRejectPackets();
  }

  /**
   * The actual lower-bound exchange-rate (sent : delivered) encountered during the payment.
   *
   * @return A {@link Ratio}.
   */
  Ratio lowerBoundExchangeRate();

  /**
   * The actual upper-bound exchange-rate (sent : delivered) encountered during the payment.
   *
   * @return A {@link Ratio}.
   */
  Ratio upperBoundExchangeRate();

  /**
   * Metadata representing the total time the STREAM payment took to complete or fail.
   *
   * @return A {@link Duration} representing the total time a STREAM payment took to complete or fail.
   */
  Duration paymentDuration();


}
