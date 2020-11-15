package org.interledger.stream.errors;

import org.interledger.stream.StreamConnection;
import org.interledger.stream.StreamConnectionId;

/**
 * A checked exception thrown when a there is no more value left to be sent for a STREAM payment.
 */
public class InsufficientStreamFundsException extends StreamException {

  private final int streamSequenceNumber;

  /**
   * Constructs a new runtime exception with {@code null} as its detail message.  The cause is not initialized, and may
   * subsequently be initialized by a call to {@link #initCause}.
   *
   * @param streamConnectionId   A {@link StreamConnectionId} that uniquely identifies the {@link StreamConnection} that
   *                             emitted this exception.
   * @param streamSequenceNumber
   */
  public InsufficientStreamFundsException(StreamConnectionId streamConnectionId, int streamSequenceNumber) {
    super(streamConnectionId);
    this.streamSequenceNumber = streamSequenceNumber;
  }

  /**
   * Constructs a new runtime exception with the specified detail message. The cause is not initialized, and may
   * subsequently be initialized by a call to {@link #initCause}.
   *
   * @param message              the detail message. The detail message is saved for later retrieval by the {@link
   *                             #getMessage()} method.
   * @param streamConnectionId   A {@link StreamConnectionId} that uniquely identifies the {@link StreamConnection}
   *                             that
   * @param streamSequenceNumber
   */
  public InsufficientStreamFundsException(String message, StreamConnectionId streamConnectionId,
      int streamSequenceNumber) {
    super(message, streamConnectionId);
    this.streamSequenceNumber = streamSequenceNumber;
  }

  /**
   * Constructs a new runtime exception with the specified detail message and cause.
   *
   * <p>Note that the detail message associated with {@code cause} is <i>not</i>
   * automatically incorporated in this runtime exception's detail message.</p>
   *
   * @param message              the detail message (which is saved for later retrieval by the {@link #getMessage()}
   *                             method).
   * @param cause                the cause (which is saved for later retrieval by the {@link #getCause()} method).  (A
   *                             {@code null} value is permitted, and indicates that the cause is nonexistent or
   *                             unknown.)
   * @param streamConnectionId   A {@link StreamConnectionId} that uniquely identifies the {@link StreamConnection} that
   *                             emitted this exception.
   * @param streamSequenceNumber
   *
   * @since 1.4
   */
  public InsufficientStreamFundsException(
      String message, Throwable cause, StreamConnectionId streamConnectionId, int streamSequenceNumber
  ) {
    super(message, cause, streamConnectionId);
    this.streamSequenceNumber = streamSequenceNumber;
  }

  /**
   * Constructs a new runtime exception with the specified cause and a detail message of {@code (cause==null ? null :
   * cause.toString())} (which typically contains the class and detail message of {@code cause}).  This constructor is
   * useful for runtime exceptions that are little more than wrappers for other throwables.
   *
   * @param cause                the cause (which is saved for later retrieval by the {@link #getCause()} method).  (A
   *                             {@code null} value is permitted, and indicates that the cause is nonexistent or
   *                             unknown.)
   * @param streamConnectionId   A {@link StreamConnectionId} that uniquely identifies the {@link StreamConnection} that
   *                             emitted this exception.
   * @param streamSequenceNumber
   *
   * @since 1.4
   */
  public InsufficientStreamFundsException(
      Throwable cause, StreamConnectionId streamConnectionId, int streamSequenceNumber
  ) {
    super(cause, streamConnectionId);
    this.streamSequenceNumber = streamSequenceNumber;
  }

  /**
   * Returns a short description of this throwable. The result is the concatenation of:
   * <ul>
   * <li> the {@linkplain Class#getName() name} of the class of this object
   * <li> ": " (a colon and a space)
   * <li> the result of invoking this object's {@link #getLocalizedMessage} method
   * <li> the result of invoking the {@link StreamConnectionId#value()} method
   * <li> the result of invoking the {@link #getStreamSequenceNumber} method
   * </ul>
   * If {@code getLocalizedMessage} returns {@code null}, then just
   * the class name is returned.
   *
   * @return a string representation of this throwable.
   */
  public String toString() {
    String str = getClass().getName();
    String message = getLocalizedMessage()
        + " streamConnectionId=" + getStreamConnectionId().value()
        + " streamSequenceNumber=" + streamSequenceNumber;
    return str + ": " + message;
  }

  public int getStreamSequenceNumber() {
    return streamSequenceNumber;
  }
}
