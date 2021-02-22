package org.interledger.stream.pay.trackers;

import com.google.common.annotations.VisibleForTesting;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A tracker to help ensure packets are transmitted at a consistent cadence in order to prevent sending more packets
 * than the network can handle.
 */
public class PacingTracker {

  /**
   * Maximum number of packets to have in-flight, yet to receive a Fulfill or Reject.
   */
  public static int MAX_INFLIGHT_PACKETS = 20;

  /**
   * Initial number of packets to send in 1 second interval (25ms delay between packets).
   */
  public static int DEFAULT_PACKETS_PER_SECOND = 40;

  /**
   * Always try to send at least 1 packet in 1 second (unless RTT is very high).
   */
  public static int MIN_PACKETS_PER_SECOND = 1;

  /**
   * Maximum number of packets to send in a 1 second interval, after ramp up (5ms delay).
   */
  public static final int MAX_PACKETS_PER_SECOND = 200;

  /**
   * RTT to use for pacing before an average can be ascertained.
   */
  private static final int DEFAULT_ROUND_TRIP_TIME_MS = 200;

  /**
   * Weight to compute next RTT average. Halves weight of past round trips every ~5 flights.
   */
  private static final float ROUND_TRIP_AVERAGE_WEIGHT = 0.9f;

  /**
   * The {@link Instant} when the most recent packet was sent.
   */
  private final AtomicReference<Instant> lastPacketSentTime = new AtomicReference<>(Instant.now());

  /**
   * Number of packets currently in flight.
   */
  private final AtomicInteger numberInFlight = new AtomicInteger();

  /**
   * Exponential weighted moving average of the round trip time.
   */
  private final AtomicInteger averageRoundTripMs = new AtomicInteger(DEFAULT_ROUND_TRIP_TIME_MS);

  /**
   * Rate of packets to send per second. This shouldn't ever be 0, but may become a small fraction.
   */
  private final AtomicInteger packetsPerSecond = new AtomicInteger(DEFAULT_PACKETS_PER_SECOND);

  /**
   * Rate to send packets, in packets / millisecond, using packet rate-limit and round trip time. Corresponds to the ms
   * delay between each packet.
   */
  public int getPacketFrequency() {
    final int packetsPerSecondSnapshot = this.getPacketsPerSecond();
    int packetsPerSecondDelay = 1000 / (packetsPerSecondSnapshot == 0 ? 1 : packetsPerSecondSnapshot);
    int maxInFlightDelay = this.getAverageRoundTripTimeMs() / MAX_INFLIGHT_PACKETS;

    return Math.max(packetsPerSecondDelay, maxInFlightDelay);
  }

  /**
   * Earliest UNIX timestamp when the pacer will allow the next packet to be sent.
   */
  public Instant getNextPacketSendTime() {
    final Duration delayDuration = Duration.of(this.getPacketFrequency(), ChronoUnit.MILLIS);
    return getLastPacketSentTime().plus(delayDuration);
  }

  /**
   * Accessor for the number of packets currently "in-flight" (i.e., waiting on a response from the receiver).
   *
   * @return An int.
   */
  public int getNumberInFlight() {
    return numberInFlight.get();
  }

  /**
   * Accessor for the time the last packet was sent from this sender.
   *
   * @return An {@link Instant}.
   */
  @VisibleForTesting
  Instant getLastPacketSentTime() {
    return this.lastPacketSentTime.get();
  }

  public void setLastPacketSentTime(final Instant lastPacketSentTime) {
    Objects.requireNonNull(lastPacketSentTime);
    this.lastPacketSentTime.set(lastPacketSentTime);
  }

  /**
   * Increase the number of packet currently "in-flight" by 1.
   */
  public void incrementNumPacketsInFlight() {
    this.numberInFlight.getAndIncrement();
  }

  /**
   * Reduce the number of packet currently "in-flight" by 1.
   */
  public void decrementNumPacketsInFlight() {
    this.numberInFlight.getAndDecrement();
  }

  /**
   * Update the average "round trip time" for a packet using a new value of {@code roundTripTimeMs}.
   *
   * @param roundTripTimeMs An integer representing the number of milliseconds that a packet took to make an entire
   *                        round-trip from this payer to the receiver and back.
   */
  public void updateAverageRoundTripTime(final int roundTripTimeMs) {
    this.averageRoundTripMs.getAndAccumulate(roundTripTimeMs, (rtt, existingValue) -> {
      float foo = (rtt * ROUND_TRIP_AVERAGE_WEIGHT) + (rtt * (1 - ROUND_TRIP_AVERAGE_WEIGHT));
      return (int) foo;
    });
  }

  /**
   * Accessor for the number of millis that a packet takes, on average, to reach the recipient and come back to the
   * sender.
   *
   * @return An int.
   */
  @VisibleForTesting
  int getAverageRoundTripTimeMs() {
    return this.averageRoundTripMs.get();
  }

  /**
   * Set the number of packets (per-second) that a sender should try not to exceed when sending packets.
   *
   * @param reducedRate The new value for the number of packets-per-second to not exceed.
   */
  public void setPacketsPerSecond(int reducedRate) {
    this.packetsPerSecond.set(reducedRate);
  }

  /**
   * Accessor for the number of packets (per-second) that a sender should try not to exceed when sending packets.
   *
   * @return An int.
   */
  public int getPacketsPerSecond() {
    return this.packetsPerSecond.get();
  }
}
