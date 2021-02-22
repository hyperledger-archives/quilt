package org.interledger.stream.pay.trackers;

import org.interledger.core.fluent.Percentage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks statistics for a given payment.
 */
public class StatisticsTracker {

  // Static because these filters will be constructed a lot.
  //private static final Logger LOGGER = LoggerFactory.getLogger(FailureFilter.class);

  /**
   * An {@link Instant} when this tracker was created (a surrogate for when the payment started).
   */
  private final Instant paymentStartInstant;

  /**
   * The total number of rejection packets received on the stream.
   */
  private final AtomicInteger numRejects;

  /**
   * The total number of fulfill packets received on the stream.
   */
  private final AtomicInteger numFulfills;

  /**
   * Required-args Constructor.
   */
  public StatisticsTracker() {
    this.paymentStartInstant = Instant.now();
    this.numRejects = new AtomicInteger(0);
    this.numFulfills = new AtomicInteger(0);
  }

  /**
   * Accessor for the number of Fulfill packets that have been encountered in this payment.
   *
   * @return An int.
   */
  public int getNumFulfills() {
    return this.numFulfills.get();
  }

  /**
   * Accessor for the number of Reject packets that have been encountered in this payment.
   *
   * @return An int.
   */
  public int getNumRejects() {
    return this.numRejects.get();
  }

  /**
   * Increment the number of fulfills.
   */
  public void incrementNumFulfills() {
    this.numFulfills.getAndIncrement();
  }

  /**
   * Increment the number of rejects.
   */
  public void incrementNumRejects() {
    this.numRejects.getAndIncrement();
  }

  /**
   * Accessor for the total number of responses thus far (rejects + fulfills).
   *
   * @return An long.
   */
  public int getTotalPacketResponses() {
    return this.getNumFulfills() + this.getNumRejects();
  }

  /**
   * Helper method to compute the packet-failure percentage for a given payment. Only starts tracking after 30 packets
   * in order to allow for a warm-up.
   *
   * @return A {@link Percentage} representing the number of rejected packets to fulfilled packets.
   */
  public Percentage computeFailurePercentage() {
    if (this.getTotalPacketResponses() <= 50) {
      return Percentage.ZERO_PERCENT;
    }
    if (this.getNumRejects() <= 0) {
      return Percentage.ZERO_PERCENT; // <-- Guards against divide by zero.
    } else {
      final double failurePercentDouble =
        ((double) this.getNumRejects()) / ((double) this.getTotalPacketResponses());
      final BigDecimal failurePercentage = new BigDecimal(failurePercentDouble).setScale(3, RoundingMode.HALF_EVEN);
      return Percentage.of(failurePercentage);
    }
  }

  /**
   * Accessor for the {@link Instant} that a pyament started.
   *
   * @return An {@link Instant}.
   */
  public Instant getPaymentStartInstant() {
    return this.paymentStartInstant;
  }
}
