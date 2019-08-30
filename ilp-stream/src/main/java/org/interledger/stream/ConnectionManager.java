package org.interledger.stream;

import org.interledger.stream.frames.ConnectionClose;
import org.interledger.stream.frames.StreamClose;

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * <p>Manages all STREAM Connections.</p>
 */
public class ConnectionManager {

  private final Map<UUID, ConnectionDetails> connections;

  public ConnectionManager() {
    this(Maps.newConcurrentMap());
  }

  public ConnectionManager(final Map<UUID, ConnectionDetails> connections) {
    this.connections = Objects.requireNonNull(connections);
  }

  /**
   * Open a connection to the "other" party to this stream.
   *
   * @return
   */
  public ConnectionDetails openConnection() {
    return null;
  }

  /**
   * <p>Close a connection.</p>
   *
   * <p>Either endpoint can close the connection using a {@link ConnectionClose} frame. Implementations MAY allow
   * half-open connections (where one side has closed the connection and the other is still able to send).</p>
   *
   * <p>ConnectionClose frames are used to communicate both normal connection closes as well as errors.</p>
   */
  public void closeConnection(final UUID streamConnectionId) {

  }

  /**
   * <p>Setup a stream with another participant.</p>
   *
   * <p>A server MUST communicate the following values to a client using an authenticated, encrypted communication
   * channel (such as HTTPS). Key exchange is NOT provided by STREAM.</p>
   *
   * <p>
   * <ol>
   *  <li>STREAM Version: (optional -- assumed to be version 1 unless specified)</li>
   *  <li>Server ILP Address</li>
   *  <li>Cryptographically secure random or pseudorandom shared secret (it is RECOMMENDED to use 32 bytes). To avoid
   * storing a 32 byte secret for each connection, a server MAY deterministically generate the shared secret for each
   * connection from a single server secret and a nonce appended to the ILP Address given to a particular client, for
   * example by using an HMAC.</li>
   * </ol>
   * </p>
   */
  public void openMoneyStream() {

    // Send a StreamMoney frame to the recever. Client streams MUST be odd-numbered starting with 1.

  }

  /**
   * <p>Allows either endpoint to close a stream using a {@link StreamClose} frame. Implementations MAY allow half-open
   * streams (where one side has closed and the other is still able to send).</p>
   *
   * <p>StreamClose frames are used to communicate both normal stream closes as well as errors.</p>
   */
  public void closeMoneyStream() {

  }

}
