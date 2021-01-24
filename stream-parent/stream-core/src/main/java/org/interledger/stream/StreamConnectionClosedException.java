package org.interledger.stream;

/**
 * A checked exception thrown when a Connection's sequence is too large or too small. This exception is checked so that
 * callers are forced to handle the error condition if a method throws it.
 *
 * @deprecated Will be removed once Stream Sender is removed. Prefer
 * {@link org.interledger.stream.connection.StreamConnectionClosedException} instead.
 */
@Deprecated
public class StreamConnectionClosedException extends StreamException {

  /**
   * Constructs a new runtime exception with {@code null} as its detail message.  The cause is not initialized, and may
   * subsequently be initialized by a call to {@link #initCause}.
   *
   * @param streamConnectionId A {@link StreamConnectionId} that uniquely identifies the {@link StreamConnection} that
   *                           emitted this exception.
   */
  public StreamConnectionClosedException(StreamConnectionId streamConnectionId) {
    super(streamConnectionId);
  }

  /**
   * Constructs a new runtime exception with the specified detail message. The cause is not initialized, and may
   * subsequently be initialized by a call to {@link #initCause}.
   *
   * @param message            the detail message. The detail message is saved for later retrieval by the {@link
   *                           #getMessage()} method.
   * @param streamConnectionId A {@link StreamConnectionId} that uniquely identifies the {@link StreamConnection} that
   *                           emitted this exception.
   */
  public StreamConnectionClosedException(String message, StreamConnectionId streamConnectionId) {
    super(message, streamConnectionId);
  }

  /**
   * Constructs a new runtime exception with the specified detail message and cause.
   *
   * <p>Note that the detail message associated with {@code cause} is <i>not</i>
   * automatically incorporated in this runtime exception's detail message.</p>
   *
   * @param message            the detail message (which is saved for later retrieval by the {@link #getMessage()}
   *                           method).
   * @param cause              the cause (which is saved for later retrieval by the {@link #getCause()} method).  (A
   *                           {@code null} value is permitted, and indicates that the cause is nonexistent or
   *                           unknown.)
   * @param streamConnectionId A {@link StreamConnectionId} that uniquely identifies the {@link StreamConnection} that
   *                           emitted this exception.
   *
   * @since 1.4
   */
  public StreamConnectionClosedException(String message, Throwable cause, StreamConnectionId streamConnectionId) {
    super(message, cause, streamConnectionId);
  }

  /**
   * Constructs a new runtime exception with the specified cause and a detail message of {@code (cause==null ? null :
   * cause.toString())} (which typically contains the class and detail message of {@code cause}).  This constructor is
   * useful for runtime exceptions that are little more than wrappers for other throwables.
   *
   * @param cause              the cause (which is saved for later retrieval by the {@link #getCause()} method).  (A
   *                           {@code null} value is permitted, and indicates that the cause is nonexistent or
   *                           unknown.)
   * @param streamConnectionId A {@link StreamConnectionId} that uniquely identifies the {@link StreamConnection} that
   *                           emitted this exception.
   *
   * @since 1.4
   */
  public StreamConnectionClosedException(Throwable cause, StreamConnectionId streamConnectionId) {
    super(cause, streamConnectionId);
  }
}
