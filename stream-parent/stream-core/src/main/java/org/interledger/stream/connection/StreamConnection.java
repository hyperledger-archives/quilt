package org.interledger.stream.connection;

import static org.interledger.core.fluent.FluentCompareTo.is;

import org.interledger.core.DateUtils;
import org.interledger.core.InterledgerAddress;
import org.interledger.fx.Denomination;
import org.interledger.stream.StreamConnectionId;
import org.interledger.stream.crypto.StreamSharedSecret;
import org.interledger.stream.model.AccountDetails;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;

import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>The session established between two endpoints that uses a single shared secret and multiplexes multiple streams
 * of money and/or data. This implementation models the connection from the perspective of a STREAM Client.</p>
 *
 * <p>Note that any given {@link StreamConnection} in a JVM manages a unique sequence id that can be incremented
 * for each Stream Packet sent over the connection.</p>
 */
public class StreamConnection implements Closeable {

  public static final UnsignedLong MAX_PACKETS_PER_CONNECTION = UnsignedLong.valueOf((long) Math.pow(2, 31));

  /**
   * The unique identifier of this Connection. A Connection is unique to a destination {@link InterledgerAddress} and a
   * {@link StreamSharedSecret}.
   */
  private final Instant creationDateTime;

  /**
   * The details of for client of this Stream payment. Clients are not required to send a source ILP address, especially
   * to support scenarios where the client is not routable (e.g., a client paying via an ILP-over-HTTP link that has no
   * incoming URL, such as from an Android device). However, in this Java implementation, an address is always supplied,
   * but is schemed `private` if it is not routable.
   */
  private final AccountDetails sourceAccountDetails;

  /**
   * The destination address of this request.
   */
  private final InterledgerAddress destinationAddress;

  /**
   * The {@link Denomination} of the destination. This value is mutable (using an {@link AtomicReference}) because it
   * typically is returned as part of the STREAM flow itself and is not available when a {@link StreamConnection} is
   * created (though in some circumstances, such as an OpenPayments server, this value can be returned via an SPSP
   * request and _may_ be known prior to constructing a {@link StreamConnection}).
   */
  private final AtomicReference<Optional<Denomination>> destinationDenomination;

  private final StreamSharedSecret streamSharedSecret;
  private final StreamConnectionId streamConnectionId;
  private final AtomicReference<UnsignedLong> sequence;
  private final AtomicReference<StreamConnectionState> connectionState;

  /**
   * Required-args Constructor that derives the {@link StreamConnectionId} from the supplied inputs. This implementation
   * derives a {@link StreamConnectionId} from the supplied {@code sharedSecret} by computing a SHA-256 hash of the
   * shared secret and the address so that the actual shared secret isn't hanging around in memory from call to call. In
   * this way the connection id will also be uniquely scoped to a receiver address plus shared secret combination.
   *
   * @param sourceAccountDetails The {@link AccountDetails} for the sender.
   * @param destinationAddress   The {@link InterledgerAddress} of the receiver of this STREAM payment.
   * @param streamSharedSecret   The {@link StreamSharedSecret} used to encrypt and decrypt packets transmitted over
   *                             this.
   */
  public StreamConnection(
    final AccountDetails sourceAccountDetails,
    final InterledgerAddress destinationAddress,
    final StreamSharedSecret streamSharedSecret
  ) {
    this.sourceAccountDetails = Objects.requireNonNull(sourceAccountDetails);
    this.destinationAddress = Objects.requireNonNull(destinationAddress);
    this.streamSharedSecret = Objects.requireNonNull(streamSharedSecret);

    this.creationDateTime = DateUtils.now();
    this.sequence = new AtomicReference<>(UnsignedLong.ONE);
    this.streamConnectionId = Objects.requireNonNull(
      StreamConnectionId.from(destinationAddress, org.interledger.core.SharedSecret.of(streamSharedSecret.key()))
    );
    this.destinationDenomination = new AtomicReference<>(Optional.empty());
    this.connectionState = new AtomicReference<>(StreamConnectionState.AVAILABLE);
  }

  /**
   * Required-args Constructor that derives the {@link StreamConnectionId} from the supplied inputs. This implementation
   * derives a {@link StreamConnectionId} from the supplied {@code sharedSecret} by computing a SHA-256 hash of the
   * shared secret and the address so that the actual shared secret isn't hanging around in memory from call to call. In
   * this way the connection id will also be uniquely scoped to a receiver address plus shared secret combination.
   *
   * @param sourceAccountDetails    The {@link AccountDetails} for the sender.
   * @param destinationAddress      The {@link InterledgerAddress} of the receiver of this STREAM payment.
   * @param streamSharedSecret      The {@link StreamSharedSecret} used to encrypt and decrypt packets transmitted over
   *                                this
   * @param destinationDenomination An optionally-present {@link Denomination} for the destination address.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public StreamConnection(
    final AccountDetails sourceAccountDetails,
    final InterledgerAddress destinationAddress,
    final StreamSharedSecret streamSharedSecret,
    final Optional<Denomination> destinationDenomination
  ) {
    this.sourceAccountDetails = Objects.requireNonNull(sourceAccountDetails);
    this.destinationAddress = Objects.requireNonNull(destinationAddress);
    this.streamSharedSecret = Objects.requireNonNull(streamSharedSecret);

    this.creationDateTime = DateUtils.now();
    this.sequence = new AtomicReference<>(UnsignedLong.ONE);
    this.streamConnectionId = Objects.requireNonNull(
      StreamConnectionId.from(destinationAddress, org.interledger.core.SharedSecret.of(streamSharedSecret.key()))
    );
    this.destinationDenomination = new AtomicReference<>(Objects.requireNonNull(destinationDenomination));
    this.connectionState = new AtomicReference<>(StreamConnectionState.AVAILABLE);
  }

  /**
   * Return the next sequence number from {@link #sequence} for use by a Stream Sender. The first call to this method
   * will return {@link UnsignedInteger#ONE}. Upon each subsequent call to this method, the sequence will increase
   * monotonically up to {2<sup>31</sup>}, after which the connection will be closed.
   *
   * @return A {@link UnsignedInteger} representing the next sequence number that can safely be used by a Stream Sender.
   *
   * @throws StreamConnectionClosedException if the sequence can no longer be safely incremented.
   */
  public UnsignedLong nextSequence() throws StreamConnectionClosedException {
    // Unique per-thread.
    final UnsignedLong nextSequence = sequence
      .getAndUpdate(currentSequence -> currentSequence.plus(UnsignedLong.ONE));
    if (sequenceIsSafeForSingleSharedSecret(nextSequence)) {
      return nextSequence;
    } else {
      this.closeConnection();
      throw new StreamConnectionClosedException(streamConnectionId);
    }
  }

  /**
   * Determines if {@code sequence} can be safely used to encrypt data using a single shared secret. Per IL-RFC-29,
   * "Implementations MUST close the connection once either endpoint has sent 2^31 packets. According to NIST, it is
   * unsafe to use AES-GCM for more than 2^32 packets using the same encryption key (STREAM uses the limit of 2^31
   * because both endpoints encrypt packets with the same key).
   *
   * @param sequence An {@link UnsignedLong} representing the current Stream sequence.
   *
   * @return {@code true} if the current sequence can safely be used with a single shared-secret; {@code false}
   *   otherwise.
   */
  @VisibleForTesting
  boolean sequenceIsSafeForSingleSharedSecret(final UnsignedLong sequence) {
    // Only return true if the Connection is not closed, and the `sequence` is below MAX_PACKETS_PER_CONNECTION (above
    // that value is unsafe).
    return !isClosed() && is(Objects.requireNonNull(sequence)).lessThanOrEqualTo(MAX_PACKETS_PER_CONNECTION);
  }

  public Instant getCreationDateTime() {
    return creationDateTime;
  }

  /**
   * <p>Transition this {@link StreamConnection} to its next state.</p>
   *
   * <p>If the current {@link #connectionState} is {@link StreamConnectionState#AVAILABLE}, then transition to this
   * Connection to {@link StreamConnectionState#OPEN}. If the current {@link #connectionState} is {@link
   * StreamConnectionState#OPEN}, then transition to this Connection to {@link StreamConnectionState#CLOSED}. Otherwise,
   * this operation is a no-op.</p>
   */
  public void transitionConnectionState() {
    this.connectionState.updateAndGet(streamConnectionState -> {
      switch (streamConnectionState) {
        case AVAILABLE: {
          return StreamConnectionState.OPEN;
        }
        case OPEN:
        case CLOSED:
        default: {
          return StreamConnectionState.CLOSED;
        }
      }
    });
  }

  public AccountDetails getSourceAccountDetails() {
    return this.sourceAccountDetails;
  }

  /**
   * Accessor for the destination address of this Stream connection.
   *
   * @return An {@link InterledgerAddress}.
   */
  public InterledgerAddress getDestinationAddress() {
    return destinationAddress;
  }

  /**
   * Accessor for the optionally-present denomination for the destination address of this Stream connection. This value
   * is optional because it is often discovered as part of a stream probe. Thus, at the beginning of a connection, this
   * value may be {@link Optional#empty()}.
   *
   * @return An optionally-present {@link Denomination}.
   */
  public Optional<Denomination> getDestinationDenomination() {
    return destinationDenomination.get();
  }

  /**
   * Accessor for the shared secret that encrypts data between a sender and receiver in this stream connection.
   *
   * @return A {@link StreamSharedSecret}.
   */
  public StreamSharedSecret getStreamSharedSecret() {
    return streamSharedSecret;
  }

  /**
   * Accessor for the current state of this stream connection.
   *
   * @return A {@link StreamConnectionState}.
   */
  public StreamConnectionState getConnectionState() {
    return connectionState.get();
  }

  /**
   * Accessor for the Stream Connection Id.
   *
   * @return A {@link StreamConnectionId} that uniquely identifies this Stream Connection.
   */
  public StreamConnectionId getStreamConnectionId() {
    return streamConnectionId;
  }

  /**
   * Transition this Stream Connection into the {@link StreamConnectionState#CLOSED}.
   */
  public synchronized void closeConnection() {
    this.connectionState.set(StreamConnectionState.CLOSED);

    // WARNING: Don't ever remove the connection once it's closed. Closed connections MUST never be re-used so that they
    // don't accidentally use a sequence number that exceeds the StreamConnection.MAX_PACKETS_PER_CONNECTION
  }

  /**
   * Helper method to determine if the connection is closed.
   *
   * @return {@code true} if this Connection is closed; {@code false} otherwise.
   */
  public boolean isClosed() {
    return this.connectionState.get() == StreamConnectionState.CLOSED;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    StreamConnection that = (StreamConnection) obj;

    return streamConnectionId.equals(that.streamConnectionId);
  }

  @Override
  public int hashCode() {
    return streamConnectionId.hashCode();
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", StreamConnection.class.getSimpleName() + "[", "]")
      .add("sourceAccountDetails=" + this.sourceAccountDetails)
      .add("destinationAddress=" + this.destinationAddress)
      .add("destinationDenomination=" + this.destinationDenomination)
      .add("creationDateTime=" + this.creationDateTime)
      .add("streamConnectionId=" + this.streamConnectionId)
      .add("sequence=" + this.sequence)
      .add("connectionState=" + this.connectionState)
      .toString();
  }

  @Override
  public void close() throws IOException {
    this.closeConnection();
  }

  /**
   * Valid states for a {@link StreamConnection}. A StreamConnection begins life in the {@link #AVAILABLE} state. Once
   * the first sequence is emitted, the Connection transitions into the {@link #OPEN} state. If a Connection is closed,
   * it can never be used again. If a Connection's sequence number exceeds a safe maximum per IL-RFC-29, the Connection
   * becomes closed.
   */
  enum StreamConnectionState {
    /**
     * The Connection is available, but has not yet been used (i.e., no calls to {@link #nextSequence()} have been made
     * on this Stream Connection).
     */
    AVAILABLE,

    /**
     * The Connection is open and available for use, and may have been used to send Stream Packets in the past depending
     * on the value of {@link #sequence}.
     */
    OPEN,

    /**
     * The Connection is closed and may no longer be used.
     */
    CLOSED
  }
}
