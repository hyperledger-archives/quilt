package org.interledger.stream.server;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.stream.StreamConnectionDetails;

/**
 * <p>A service that fulfills incoming STREAM packets.</p>
 */
public interface StreamServer {

  /**
   * <p>Setup a stream with another participant.</p>
   *
   * <p>A server MUST communicate the following values to a client using an authenticated, encrypted communication
   * channel (such as HTTPS). Key exchange is NOT provided by STREAM.</p>
   *
   * @param receiverAddress The {@link InterledgerAddress} of the receiver.
   *
   * @return A {@link StreamConnectionDetails} that is uniquely generated on every request.
   */
  StreamConnectionDetails setupStream(InterledgerAddress receiverAddress);

  /**
   * Receive money on behalf of {@code clientAddress} using the STREAM protocol.
   *
   * @param preparePacket The actual {@link InterledgerPreparePacket} with a {@link InterledgerPreparePacket#getDestination()}
   *                      that includes information that could only have been created by this receiver.
   * @param clientAddress A {@link InterledgerAddress} of the account this packet should be delivered to.
   */
  InterledgerResponsePacket receiveMoney(
      final InterledgerPreparePacket preparePacket, final InterledgerAddress clientAddress
  );

}