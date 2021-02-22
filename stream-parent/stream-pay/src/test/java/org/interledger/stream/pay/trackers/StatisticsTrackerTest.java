package org.interledger.stream.pay.trackers;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.core.fluent.Percentage;

import org.junit.Test;

import java.math.BigDecimal;

/**
 * Unit tests for {@link StatisticsTracker}.
 */
public class StatisticsTrackerTest {

  private StatisticsTracker statisticsTracker = new StatisticsTracker();

  @Test
  public void incrementAndGetNumFulfills() {
    assertThat(statisticsTracker.getNumFulfills()).isEqualTo(0);
    statisticsTracker.incrementNumFulfills();
    assertThat(statisticsTracker.getNumFulfills()).isEqualTo(1);
  }

  @Test
  public void incrementAndGetNumRejects() {
    assertThat(statisticsTracker.getNumRejects()).isEqualTo(0);
    statisticsTracker.incrementNumRejects();
    assertThat(statisticsTracker.getNumRejects()).isEqualTo(1);
  }

  @Test
  public void getTotalResponsePackets() {
    assertThat(statisticsTracker.getTotalPacketResponses()).isEqualTo(0);
    statisticsTracker.incrementNumRejects();
    assertThat(statisticsTracker.getTotalPacketResponses()).isEqualTo(1);
    statisticsTracker.incrementNumFulfills();
    assertThat(statisticsTracker.getTotalPacketResponses()).isEqualTo(2);
  }

  //////////////////////
  // computeFailurePercentage
  //////////////////////

  @Test
  public void computeFailurePercentageWhen49TotalPackets() {
    for (int i = 0; i < 5; i++) {
      statisticsTracker.incrementNumRejects();
    }
    for (int i = 0; i < 44; i++) {
      statisticsTracker.incrementNumFulfills();
    }
    assertThat(statisticsTracker.computeFailurePercentage()).isEqualTo(Percentage.ZERO_PERCENT);
  }

  @Test
  public void computeFailurePercentageWhe50TotalPackets() {
    for (int i = 0; i < 5; i++) {
      statisticsTracker.incrementNumRejects();
    }
    for (int i = 0; i < 45; i++) {
      statisticsTracker.incrementNumFulfills();
    }
    assertThat(statisticsTracker.computeFailurePercentage()).isEqualTo(Percentage.ZERO_PERCENT);
  }

  @Test
  public void computeFailurePercentageWhe51TotalPackets() {
    for (int i = 0; i < 5; i++) {
      statisticsTracker.incrementNumRejects();
    }
    for (int i = 0; i < 46; i++) {
      statisticsTracker.incrementNumFulfills();
    }
    assertThat(statisticsTracker.computeFailurePercentage()).isEqualTo(Percentage.of(new BigDecimal(".098")));
  }

  @Test
  public void computeFailurePercentageWhenVeryHighPctFailure() {
    for (int i = 0; i < 100; i++) {
      statisticsTracker.incrementNumRejects();
    }
    for (int i = 0; i < 6; i++) {
      statisticsTracker.incrementNumFulfills();
    }

    assertThat(statisticsTracker.computeFailurePercentage()).isEqualTo(Percentage.of(new BigDecimal("0.943")));
  }

  @Test
  public void computeFailurePercentageWhenNoPackets() {
    assertThat(statisticsTracker.computeFailurePercentage()).isEqualTo(Percentage.ZERO_PERCENT);
  }
}