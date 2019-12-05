package org.interledger.stream.sender;

import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.link.Link;
import org.interledger.link.exceptions.LinkRetriesExceededException;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Allows configuration of backoff behavior for sending {@link InterledgerPreparePacket}s to avoid overwhelming the
 * network in the event a retryable error is encountered.
 */
public interface BackoffController {

  /**
   * Send the prepare packet using the backoff behavior configured in this controller.
   * @param link                the {@link Link} used to send the packet
   * @param preparePacket       the {@link InterledgerPreparePacket} to be sent
   * @param numRejectedPackets  incremented by the number of rejected packets encountered while retrying
   * @return the response packet (which may be the result of 0 or more retries)
   * @throws LinkRetriesExceededException when the maximum number of configured retries is exceeded
   */
  InterledgerResponsePacket sendWithBackoff(Link link, InterledgerPreparePacket preparePacket,
                                            AtomicInteger numRejectedPackets) throws LinkRetriesExceededException;

}
