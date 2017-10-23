package org.interledger.ilqp;

import org.interledger.InterledgerPacket;

import java.time.Duration;

/**
 * A parent interface for all quote responses in ILQP.
 */
public interface QuoteResponse extends InterledgerPacket {

  /**
   * How long the sender should put money on hold.
   *
   * @return An instance of {@link Duration}.
   */
  Duration getSourceHoldDuration();

}
