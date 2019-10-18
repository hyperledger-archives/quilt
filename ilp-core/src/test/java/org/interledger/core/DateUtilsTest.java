package org.interledger.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.time.temporal.ChronoField;

public class DateUtilsTest {

  @Test
  public void now() {
    // run it more than once to make sure we don't happen to run when actual system time is 000 microseconds
    for (int i  = 0; i < 100; i++) {
      // Instant.get() method only lets us get the total milliseconds or total microseconds but what
      // we want to validate is that the microsecond portion of the Instant is 0.
      // To do that, mod the microseconds by 1000 which will give us just the microsecond part
      long micros = DateUtils.now().get(ChronoField.MICRO_OF_SECOND);
      assertThat(micros % 1000).isEqualTo(0);
    }
  }

}