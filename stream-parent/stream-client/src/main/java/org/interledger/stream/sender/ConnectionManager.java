package org.interledger.stream.sender;

import org.interledger.stream.StreamConnection;
import org.interledger.stream.StreamConnectionId;

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Semaphore;

/**
 * <p>Manages all STREAM connections across this JVM.</p>
 *
 * <p>Connection management in STREAM is somewhat complicated by the fact that connection identifiers can be derieved
 * from a destination address and shared secret. These values are often stored in a sender and/or receiver, so it can be
 * easy for implementations to accidentally re-use connection identifiers without knowing it.</p>
 *
 * <p>For example, a Stream Sender that allows for simultaneous calls to `sendMoney` might guard its internal sequence
 * to make it thread-safe, but another instance might accidentally be constructed at the same time. These two
 * implementations' sequence might collide in this case.</p>
 *
 * <p>This implementation provides a JVM-wide location to open and close connections so that no two instances of a
 * Stream sender or receiver accidentally use sequences that might overlap. While this implementation does not guard
 * against two classes in _different_ JVMs using the same destination address and secret, and thus potentially
 * colliding, this possibility is considered to be extremely remote since the chances of two different JVMs using the
 * same shared secret remote. However, care should be taken when designing clustered STREAM senders and receivers
 * because it is possible that certain implementations of Stream Receiver might return the same shared secret if queried
 * more than once using the same receiver address.</p>
 */
public class ConnectionManager {

  // Ensures that multiple threads and/or implementations can share a StreamConnection and operate safely in parallel.

  /**
   * * @param connectionSequences     A {@link Map} of {@link Semaphore} keyed by the hash of an actual SharedSecret.
   *      *                                This ensures that while multiple {@link #sendMoney} requests can operate in
   *      *                                parallel, only a single {@link #sendMoney} call per Connection (i.e.,
   *      *                                SharedSecret) may be executed at a single time in order to ensure a monotonically
   *      *                                increasing sequence number for a given connection. This implementation stores a
   *      *                                sha256 hash of the shared secret so that the actual shared secret isn't hanging
   *      *                                around in memory from call to call.
   */
  private static final Map<StreamConnectionId, StreamConnection> connections = Maps.newConcurrentMap();

  public StreamConnection openConnection(final StreamConnectionId streamConnectionId) {
    Objects.requireNonNull(streamConnectionId);

    // If the connection is already open, then return it. Otherwise, return a new Connection.
    return connections.computeIfAbsent(streamConnectionId, StreamConnection::new);
  }

  /**
   * <p>Close the {@link StreamConnection} identified by {@code streamConnectionId}. This method is thread-safe because
   * the underlying {@link StreamConnection} is itself thread-safe, and will not allow emitted sequences that are
   * invalid.</p>
   *
   * <p>One concurrency-related note is that there is a small moment of time after this method has been called, but
   * where it has not yet processed the close request. During this time, an in-use Connection will process calls to
   * {@link StreamConnection#nextSequence()} so long as the sequence is not too large. Ultimately it is the
   * responsibility of {@link StreamConnection} to manage it effective Connection state.</p>
   *
   * @param streamConnectionId
   *
   * @return
   */
  public Optional<StreamConnection> closeConnection(final StreamConnectionId streamConnectionId) {
    Objects.requireNonNull(streamConnectionId);

    // Remove the connection, if it's present.
    final Optional<StreamConnection> connectionToClose = Optional.ofNullable(
        connections.remove(streamConnectionId)
    );
    // Close the Connection if it's present.
    connectionToClose.ifPresent(StreamConnection::closeConnection);
    return connectionToClose;
  }

}
