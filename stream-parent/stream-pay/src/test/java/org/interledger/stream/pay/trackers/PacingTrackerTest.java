package org.interledger.stream.pay.trackers;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit tests for {@link PacingTracker}.
 */
public class PacingTrackerTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private PacingTracker pacingTracker;
  @Before
  public void setUp() throws Exception {
    this.pacingTracker = new PacingTracker();
  }

  @Test
  public void getPacketFrequency() {
  }

  @Test
  public void getNextPacketSendTime() {
  }

  @Test
  public void getNumberInFlight() {
  }

  @Test
  public void setLastPacketSentTime() {
  }

  @Test
  public void incrementNumPacketsInFlight() {
  }

  @Test
  public void decrementNumPacketsInFlight() {
  }

  @Test
  public void updateAverageRoundTripTime() {
  }

  @Test
  public void setPacketsPerSecond() {
  }

  @Test
  public void getPacketsPerSecond() {
  }
}