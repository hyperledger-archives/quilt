package org.interledger.stream.connection;

import org.interledger.core.InterledgerAddress;
import org.interledger.stream.StreamConnectionId;
import org.interledger.stream.crypto.SharedSecret;
import org.interledger.stream.model.AccountDetails;

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>Manages all STREAM connections across this JVM.</p>
 *
 * <p>Connection management in STREAM is somewhat complicated by the fact that connection identifiers can be derived
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
 * same shared secret are considered to be remote. However, care should be taken when designing clustered STREAM senders
 * and receivers because it is possible that certain implementations of Stream Receiver might return the same shared
 * secret if queried more than once using the same receiver address.</p>
 */
public class StreamConnectionManager {

  /**
   * A {@link Map} of {@link StreamConnection} keyed by a {@link StreamConnectionId}. This ensures that while multiple
   * consumers of a Stream connection can operate in parallel, the underlying {@link StreamConnection} will guarantee a
   * monotonically increasing sequence number so that parallel Streams across multiple instances of the same connection
   * will not interfere with each other.
   */
  private static final Map<StreamConnectionId, StreamConnection> connections = Maps.newConcurrentMap();

  /**
   * Open a new {@link StreamConnection} for the specified {@code streamConnectionId}, or return an existing stream
   * connection if one has already been opened for this connection id.
   *
   * @param sourceAccountDetails A {@link StreamConnectionId} that uniquely identifies a {@link StreamConnection}.
   *                             Implementations should derive this identifier from a STREAM receiver address and shared
   *                             secret combination, which per IL-RFC-29, will share a sequence number and should thus
   *                             be considered to be a part of the same STREAM Connection.
   * @param destinationAddress   An {@link InterledgerAddress} for the destination of this payment (will be used with
   *                             the SharedSecret to construct a receiver address).
   * @param sharedSecret         A {@link SharedSecret} obtained from a setup protocol (e.g., SPSP).
   *
   * @return A {@link StreamConnection} that may have been newly constructed, or may have existed in this manager.
   */
  public StreamConnection openConnection(
    final AccountDetails sourceAccountDetails,
    final InterledgerAddress destinationAddress,
    final SharedSecret sharedSecret
  ) {
    Objects.requireNonNull(sourceAccountDetails);
    Objects.requireNonNull(destinationAddress);
    Objects.requireNonNull(sharedSecret);

    // If the connection is already open, then return it. Otherwise, return a new Connection.
    final StreamConnectionId streamConnectionId = StreamConnectionId.from(destinationAddress, sharedSecret);
    return connections.computeIfAbsent(streamConnectionId, $ ->
      new StreamConnection(sourceAccountDetails, destinationAddress, sharedSecret)
    );
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
   * @param streamConnectionId A {@link StreamConnectionId} that uniquely identifies a stream connection to close.
   *
   * @return An optionally-present {@link StreamConnection}, if one is open. Otherwise, {@link Optional#empty()}.
   */
  public Optional<StreamConnection> closeConnection(final StreamConnectionId streamConnectionId) {
    Objects.requireNonNull(streamConnectionId);

    // WARNING: Don't ever remove the connection once it's closed. Closed connections MUST never be re-used so that they
    // don't accidentally use a sequence number that exceeds the StreamConnection.MAX_FRAMES_PER_CONNECTION
    return Optional.ofNullable(connections.get(streamConnectionId))
      .map(connectionToClose -> {
        connectionToClose.closeConnection();
        return connectionToClose;
      });
  }
}
