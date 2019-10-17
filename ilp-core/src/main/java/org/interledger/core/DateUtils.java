package org.interledger.core;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Date related utilities.
 */
public class DateUtils {

  /**
   * Gets the current Instant.
   *
   * @return now
   */
  public static Instant now() {
    return Instant.now().truncatedTo(ChronoUnit.MILLIS);
  }

}
