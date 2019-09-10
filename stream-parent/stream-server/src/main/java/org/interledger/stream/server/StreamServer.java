package org.interledger.stream.server;

import org.interledger.core.InterledgerAddress;
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


}
