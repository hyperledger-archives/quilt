package org.interledger.stream.pay.trackers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

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

  /////////////////
  // getPacketFrequency
  /////////////////

  // 0 tests

  @Test
  public void getPacketFrequencyWhenPpsIsZeroAndAvgIsZero() {
    this.pacingTracker = new PacingTracker() {

      @Override
      public int getPacketsPerSecond() {
        return 0;
      }

      @Override
      int getAverageRoundTripTimeMs() {
        return 0;
      }
    };

    assertThat(pacingTracker.getPacketFrequency()).isEqualTo(1000);
  }

  @Test
  public void getPacketFrequencyWhenPpsIsZeroAndAvgIsOne() {
    this.pacingTracker = new PacingTracker() {

      @Override
      public int getPacketsPerSecond() {
        return 0;
      }

      @Override
      int getAverageRoundTripTimeMs() {
        return 1;
      }
    };

    assertThat(pacingTracker.getPacketFrequency()).isEqualTo(1000);
  }

  @Test
  public void getPacketFrequencyWhenPpsIsOneAndAvgIsZero() {
    this.pacingTracker = new PacingTracker() {

      @Override
      public int getPacketsPerSecond() {
        return 1;
      }

      @Override
      int getAverageRoundTripTimeMs() {
        return 0;
      }
    };

    assertThat(pacingTracker.getPacketFrequency()).isEqualTo(1000);
  }

  @Test
  public void getPacketFrequencyWhenPpsIsOneAndAvgIsOne() {
    this.pacingTracker = new PacingTracker() {

      @Override
      public int getPacketsPerSecond() {
        return 1;
      }

      @Override
      int getAverageRoundTripTimeMs() {
        return 1;
      }
    };

    assertThat(pacingTracker.getPacketFrequency()).isEqualTo(1000);
  }

  @Test
  public void getPacketFrequencyWhenPpsIs1000AndAvgIsZero() {
    this.pacingTracker = new PacingTracker() {

      @Override
      public int getPacketsPerSecond() {
        return 1000;
      }

      @Override
      int getAverageRoundTripTimeMs() {
        return 1;
      }
    };

    assertThat(pacingTracker.getPacketFrequency()).isEqualTo(1);
  }

  @Test
  public void getPacketFrequencyWhenPpsIsOneAndAvgIs1000() {
    this.pacingTracker = new PacingTracker() {

      @Override
      public int getPacketsPerSecond() {
        return 1;
      }

      @Override
      int getAverageRoundTripTimeMs() {
        return 1000;
      }
    };

    assertThat(pacingTracker.getPacketFrequency()).isEqualTo(1000);
  }

  @Test
  public void getPacketFrequencyWhenPpsIsOneAndAvgIs1001() {
    this.pacingTracker = new PacingTracker() {

      @Override
      public int getPacketsPerSecond() {
        return 1;
      }

      @Override
      int getAverageRoundTripTimeMs() {
        return 1001;
      }
    };

    assertThat(pacingTracker.getPacketFrequency()).isEqualTo(1000);
  }

  @Test
  public void getPacketFrequencyWhenPpsIs1001AndAvgIs1() {
    this.pacingTracker = new PacingTracker() {

      @Override
      public int getPacketsPerSecond() {
        return 1001;
      }

      @Override
      int getAverageRoundTripTimeMs() {
        return 1;
      }
    };

    assertThat(pacingTracker.getPacketFrequency()).isEqualTo(0);
  }

  @Test
  public void getPacketFrequencyWhenPpsIs1000AndAvgIs20() {
    this.pacingTracker = new PacingTracker() {

      @Override
      public int getPacketsPerSecond() {
        return 1000;
      }

      @Override
      int getAverageRoundTripTimeMs() {
        return 20;
      }
    };

    assertThat(pacingTracker.getPacketFrequency()).isEqualTo(1);
  }

  @Test
  public void getPacketFrequencyWhenPpsIs1001AndAvgIs20() {
    this.pacingTracker = new PacingTracker() {

      @Override
      public int getPacketsPerSecond() {
        return 1001;
      }

      @Override
      int getAverageRoundTripTimeMs() {
        return 20;
      }
    };

    assertThat(pacingTracker.getPacketFrequency()).isEqualTo(1);
  }

  @Test
  public void getPacketFrequencyWhenPpsIs1000AndAvgIs21() {
    this.pacingTracker = new PacingTracker() {

      @Override
      public int getPacketsPerSecond() {
        return 1000;
      }

      @Override
      int getAverageRoundTripTimeMs() {
        return 21;
      }
    };

    assertThat(pacingTracker.getPacketFrequency()).isEqualTo(1);
  }

  @Test
  public void getPacketFrequency50() {
    this.pacingTracker = new PacingTracker() {

      @Override
      public int getPacketsPerSecond() {
        return 20;
      }

      @Override
      int getAverageRoundTripTimeMs() {
        return 20;
      }
    };

    assertThat(pacingTracker.getPacketFrequency()).isEqualTo(50);
  }

  @Test
  public void getPacketFrequencyWhenPpsIs1001AndAvgIs21() {
    this.pacingTracker = new PacingTracker() {

      @Override
      public int getPacketsPerSecond() {
        return 1001;
      }

      @Override
      int getAverageRoundTripTimeMs() {
        return 21;
      }
    };

    assertThat(pacingTracker.getPacketFrequency()).isEqualTo(1);
  }

  /////////////////
  // getNextPacketSendTime
  /////////////////

  @Test
  public void getNextPacketSendTimeWhenFreqIs0() {
    this.pacingTracker = new PacingTracker() {

      @Override
      public int getPacketFrequency() {
        return 0;
      }

      @Override
      public Instant getLastPacketSentTime() {
        return Instant.ofEpochSecond(100);
      }
    };

    assertThat(pacingTracker.getNextPacketSendTime()).isEqualTo(Instant.ofEpochSecond(100));
  }

  @Test
  public void getNextPacketSendTimeWhenFreqIs1() {
    final Instant lastTime = Instant.ofEpochSecond(100);
    this.pacingTracker = new PacingTracker() {

      @Override
      public int getPacketFrequency() {
        return 0;
      }

      @Override
      public Instant getLastPacketSentTime() {
        return lastTime;
      }
    };

    assertThat(pacingTracker.getNextPacketSendTime()).isEqualTo(lastTime);
  }

  @Test
  public void getNextPacketSendTimeWhenFreqIs61() {
    final Instant lastTime = Instant.ofEpochSecond(100);
    this.pacingTracker = new PacingTracker() {

      @Override
      public int getPacketFrequency() {
        return 61;
      }

      @Override
      public Instant getLastPacketSentTime() {
        return lastTime;
      }
    };

    assertThat(pacingTracker.getNextPacketSendTime()).isEqualTo(lastTime.plus(61, ChronoUnit.MILLIS));
  }

  @Test
  public void getNextPacketSendTimeWhenFreqIs1000() {
    final Instant lastTime = Instant.ofEpochSecond(100);
    this.pacingTracker = new PacingTracker() {

      @Override
      public int getPacketFrequency() {
        return 1000;
      }

      @Override
      public Instant getLastPacketSentTime() {
        return lastTime;
      }
    };

    assertThat(pacingTracker.getNextPacketSendTime()).isEqualTo(lastTime.plusSeconds(1));
  }

  /////////////////
  // incrementNumPacketsInFlight
  /////////////////

  @Test
  public void incrementAndDecrementNumPacketsInFlight() {
    for (int i = 0; i < 1000; i++) {
      pacingTracker.incrementNumPacketsInFlight();
      assertThat(pacingTracker.getNumberInFlight()).isEqualTo(i + 1);
    }

    for (int i = 1000; i > 0; i--) {
      pacingTracker.decrementNumPacketsInFlight();
      assertThat(pacingTracker.getNumberInFlight()).isEqualTo(i - 1);
    }
  }

  /////////////////
  // updateAverageRoundTripTime
  /////////////////

  @Test
  public void updateAverageRoundTripTimeWith0() {
    pacingTracker.updateAverageRoundTripTime(0);
    assertThat(pacingTracker.getAverageRoundTripTimeMs()).isEqualTo(200);
  }

  @Test
  public void updateAverageRoundTripTimeWith1() {
    pacingTracker.updateAverageRoundTripTime(0);
    assertThat(pacingTracker.getAverageRoundTripTimeMs()).isEqualTo(200);
  }

  @Test
  public void updateAverageRoundTripTimeWith1000() {
    pacingTracker.updateAverageRoundTripTime(1000);
    assertThat(pacingTracker.getAverageRoundTripTimeMs()).isEqualTo(200);
  }

  @Test
  public void updateAverageRoundTripTimeInLoop() {
    for (int i = 0; i < 1000; i++) {
      pacingTracker.updateAverageRoundTripTime(1000);
    }
    assertThat(pacingTracker.getAverageRoundTripTimeMs()).isEqualTo(200);
  }

  @Test
  public void getAndSetPacketsPerSecond() {
    assertThat(pacingTracker.getPacketsPerSecond()).isEqualTo(40);
    pacingTracker.setPacketsPerSecond(100);
    assertThat(pacingTracker.getPacketsPerSecond()).isEqualTo(100);
  }

  @Test
  public void getAndSetLastPacketSentTime() {
    Instant newTime = Instant.now().plusSeconds(20);
    pacingTracker.setLastPacketSentTime(newTime);
    assertThat(pacingTracker.getLastPacketSentTime()).isEqualTo(newTime);
  }

}