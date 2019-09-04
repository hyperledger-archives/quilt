package org.interledger.link.http;

import static org.interledger.link.PingLoopbackLink.PING_PROTOCOL_CONDITION;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerConstants;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Objects;

/**
 * Encapsulates how to send data over an ILP-over-Http connection.
 */
public interface HttpSender {

  // Used by IlpOverHttp to test the connection by verifying a rejection with a T01.
  InterledgerPreparePacket UNFULFILLABLE_PACKET = InterledgerPreparePacket.builder()
      .executionCondition(InterledgerConstants.ALL_ZEROS_CONDITION)
      .expiresAt(Instant.now().plusSeconds(30))
      .destination(InterledgerAddress.of("peer.ilp_over_http_connection_test_that_should_always_reject"))
      .build();

  /**
   * Send an ILP prepare packet to the remote BLAST peer.
   *
   * @param preparePacket
   *
   * @return An {@link InterledgerResponsePacket}. Note that if the request to the remote peer times-out, then the ILP
   *     reject packet will contain a {@link InterledgerRejectPacket#getTriggeredBy()} that matches this node's operator
   *     address.
   */
  InterledgerResponsePacket sendPacket(final InterledgerPreparePacket preparePacket);

  /**
   * Send a very-small value payment to the destination and expect an ILP fulfillment, which demonstrates this sender
   * has send-data ping to the indicated destination address.
   *
   * @param destinationAddress
   */
  default InterledgerResponsePacket ping(final InterledgerAddress destinationAddress, final BigInteger pingAmount) {
    Objects.requireNonNull(destinationAddress);

    final InterledgerPreparePacket pingPacket = InterledgerPreparePacket.builder()
        .executionCondition(PING_PROTOCOL_CONDITION)
        // TODO: Make this timeout configurable!
        .expiresAt(Instant.now().plusSeconds(30))
        .amount(pingAmount)
        .destination(destinationAddress)
        .build();

    return this.sendPacket(pingPacket);
  }

  /**
   * <p>Check the `/ilp` endpoint for ping by making an HTTP Head request with a ping packet, and
   * asserting the values returned are one of the supported content-types required for BLAST.</p>
   *
   * <p>If the endpoint does not support producing BLAST responses, we expect a 406 NOT_ACCEPTABLE response. If the
   * endpoint does not support BLAST requests, then we expect a 415 UNSUPPORTED_MEDIA_TYPE.</p>
   */
  void testConnection();

}
