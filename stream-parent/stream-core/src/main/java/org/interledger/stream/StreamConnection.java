package org.interledger.stream;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.SharedSecret;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.UnsignedLong;

import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>The session established between two endpoints that uses a single shared secret and multiplexes multiple streams
 * of money and/or data.</p>
 *
 * <p>Any given Connection in a JVM manages a unique sequence id that can be incremented for each Stream Packet sent
 * over the connection.</p>
 */
public class StreamConnection implements Closeable {

  // NOTE: Integer.MAX_VALUE is 1 less than what we want for our Max per IL-RFC-29.
  public static final UnsignedLong MAX_FRAMES_PER_CONNECTION =
      UnsignedLong.valueOf((long) Integer.MAX_VALUE + 1L);

  /**
   * The unique identifier of this Connection. A Connection is unique to a destination {@link InterledgerAddress} and a
   * {@link SharedSecret}.
   */
  private final Instant creationDateTime;
  private final StreamConnectionId streamConnectionId;
  private final AtomicReference<UnsignedLong> sequence;
  private final AtomicReference<StreamConnectionState> connectionState;

  /**
   * Required-args Constructor that derives the {@link StreamConnectionId} from the supplied inputs. This implementation
   * derives a {@link StreamConnectionId} from the supplied {@code sharedSecret} by computing a SHA-256 hash of the
   * shared secret and the address so that the actual shared secret isn't hanging around in memory from call to call. In
   * this way the connection id will also be uniquely scoped to a receiver address plus shared secret combination.
   *
   * @param receiverAddress The {@link InterledgerAddress} of the receiver of this STREAM payment.
   * @param sharedSecret    The {@link SharedSecret} used to encrypt and decrypt packets transmitted over this
   *                        connection.
   */
  public StreamConnection(final InterledgerAddress receiverAddress, final SharedSecret sharedSecret) {
    this(StreamConnectionId.from(receiverAddress, sharedSecret));
  }

  /**
   * Required-args Constructor.
   *
   * @param streamConnectionId A {@link StreamConnectionId} that is unique to this JVM.
   */
  public StreamConnection(final StreamConnectionId streamConnectionId) {
    this.creationDateTime = Instant.now();
    this.streamConnectionId = Objects.requireNonNull(streamConnectionId, "streamConnectionId must not be null");
    this.sequence = new AtomicReference<>(UnsignedLong.ONE);
    this.connectionState = new AtomicReference<>(StreamConnectionState.AVAILABLE);
  }

  /**
   * Return the next sequence number from {@link #sequence} for use by a Stream Sender. The first call to this method
   * will return {@link UnsignedLong#ONE}. Upon each subsequent call to this method, the sequence will increase
   * monotonically up to {2<sup>31</sup>}, after which the connection will be closed.
   *
   * @return A {@link UnsignedLong} representing the next sequence number that can safely be used by a Stream Sender.
   *
   * @throws StreamConnectionClosedException if the sequence can no longer be safely incremented.
   */
  public UnsignedLong nextSequence() throws StreamConnectionClosedException {
    // Unique per-thread.
    final UnsignedLong nextSequence = sequence.getAndUpdate(currentSequence -> currentSequence.plus(UnsignedLong.ONE));
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
   * @return {@code true} if the current sequence can safely be used with a single shared-secret; {@code false}
   *     otherwise.
   */
  @VisibleForTesting
  boolean sequenceIsSafeForSingleSharedSecret(final UnsignedLong sequence) {
    // Only return true if the Connection is not closed, and the `sequence` is below MAX_FRAMES_PER_CONNECTION (above
    // that value is unsafe).
    return !isClosed() && Objects.requireNonNull(sequence).compareTo(MAX_FRAMES_PER_CONNECTION) <= 0;
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
  public void closeConnection() {
    this.connectionState.set(StreamConnectionState.CLOSED);
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
        .add("creationDateTime=" + creationDateTime)
        .add("streamConnectionId=" + streamConnectionId)
        .add("sequence=" + sequence)
        .add("connectionState=" + connectionState)
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
