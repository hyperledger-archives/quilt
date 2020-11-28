package org.interledger.stream.pay.trackers;

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
  public static int MAX_PACKETS_PER_SECOND = 200;

  /**
   * RTT to use for pacing before an average can be ascertained.
   */
  private static int DEFAULT_ROUND_TRIP_TIME_MS = 200;

  /**
   * Weight to compute next RTT average. Halves weight of past round trips every ~5 flights.
   */
  private static float ROUND_TRIP_AVERAGE_WEIGHT = 0.9f;

  /**
   * The {@link Instant} when the most recent packet was sent.
   */
  private AtomicReference<Instant> lastPacketSentTime = new AtomicReference<>(Instant.now());

  /**
   * Number of packets currently in flight.
   */
  private AtomicInteger numberInFlight = new AtomicInteger();

  /**
   * Exponential weighted moving average of the round trip time.
   */
  private AtomicInteger averageRoundTrip = new AtomicInteger(DEFAULT_ROUND_TRIP_TIME_MS);

  /**
   * Rate of packets to send per second. This shouldn't ever be 0, but may become a small fraction.
   */
  private AtomicInteger packetsPerSecond = new AtomicInteger(DEFAULT_PACKETS_PER_SECOND);

  /**
   * Rate to send packets, in packets / millisecond, using packet rate-limit and round trip time. Corresponds to the ms
   * delay between each packet.
   */
  public int getPacketFrequency() {
    int packetsPerSecondDelay = 1000 / this.packetsPerSecond.get();
    int maxInFlightDelay = this.averageRoundTrip.get() / MAX_INFLIGHT_PACKETS;

    return Math.max(packetsPerSecondDelay, maxInFlightDelay);
  }
// TODO: Unit tests

  /**
   * Earliest UNIX timestamp when the pacer will allow the next packet to be sent
   */
  public Instant getNextPacketSendTime() {
    final Duration delayDuration = Duration.of(this.getPacketFrequency(), ChronoUnit.MILLIS);
    return this.lastPacketSentTime.get().plus(delayDuration);
  }

  public int getNumberInFlight() {
    return numberInFlight.get();
  }

  public void setLastPacketSentTime(final Instant lastPacketSentTime) {
    Objects.requireNonNull(lastPacketSentTime);
    this.lastPacketSentTime.set(lastPacketSentTime);
  }

  public void incrementNumPacketsInFlight() {
    this.numberInFlight.getAndIncrement();
  }

  public void decrementNumPacketsInFlight() {
    this.numberInFlight.getAndDecrement();
  }

  // TODO: Unit test!
  public void updateAverageRoundTripTime(final int roundTripTime) {
    this.averageRoundTrip.getAndAccumulate(roundTripTime, (rtt, existingValue) -> {
      float foo = (rtt * ROUND_TRIP_AVERAGE_WEIGHT) + (rtt * (1 - ROUND_TRIP_AVERAGE_WEIGHT));
      return (int) foo;
    });
  }

  public void setPacketsPerSecond(int reducedRate) {
    this.packetsPerSecond.set(reducedRate);
  }

  public int getPacketsPerSecond() {
    return this.packetsPerSecond.get();
  }
}
