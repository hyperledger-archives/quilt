package org.interledger.stream;

import java.util.Optional;

/**
 * A checked exception thrown when a Connection's sequence is too large or too small. This exception is checked so that
 * callers are forced to handle the error condition if a method throws it.
 */
public class StreamException extends RuntimeException {

  private static final StreamConnectionId NO_STREAM_ID = StreamConnectionId.of("n/a");

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private final Optional<StreamConnectionId> streamConnectionId;

  /**
   * Constructs a new runtime exception with {@code null} as its detail message.  The cause is not initialized, and may
   * subsequently be initialized by a call to {@link #initCause}.
   */
  public StreamException() {
    this.streamConnectionId = Optional.empty();
  }

  /**
   * Constructs a new runtime exception with the specified detail message. The cause is not initialized, and may
   * subsequently be initialized by a call to {@link #initCause}.
   *
   * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()}
   *                method.
   */
  public StreamException(String message) {
    super(message);
    this.streamConnectionId = Optional.empty();
  }

  /**
   * Constructs a new runtime exception with the specified detail message and cause.
   *
   * <p>Note that the detail message associated with {@code cause} is <i>not</i> automatically
   * incorporated in this runtime exception's detail message.</p>
   *
   * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
   * @param cause   the cause (which is saved for later retrieval by the {@link #getCause()} method).  (A {@code null}
   *                value is permitted, and indicates that the cause is nonexistent or unknown.)
   *
   * @since 1.4
   */
  public StreamException(String message, Throwable cause) {
    super(message, cause);
    this.streamConnectionId = Optional.empty();
  }

  /**
   * Constructs a new runtime exception with the specified cause and a detail message of {@code (cause==null ? null :
   * cause.toString())} (which typically contains the class and detail message of {@code cause}).  This constructor is
   * useful for runtime exceptions that are little more than wrappers for other throwables.
   *
   * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).  (A {@code null}
   *              value is permitted, and indicates that the cause is nonexistent or unknown.)
   *
   * @since 1.4
   */
  public StreamException(Throwable cause) {
    super(cause);
    this.streamConnectionId = Optional.empty();
  }

  /**
   * Constructs a new runtime exception with the specified detail message, cause, suppression enabled or disabled, and
   * writable stack trace enabled or disabled.
   *
   * @param message            the detail message.
   * @param cause              the cause.  (A {@code null} value is permitted, and indicates that the cause is
   *                           nonexistent or unknown.)
   * @param enableSuppression  whether or not suppression is enabled or disabled
   * @param writableStackTrace whether or not the stack trace should be writable
   *
   * @since 1.7
   */
  protected StreamException(String message, Throwable cause, boolean enableSuppression,
    boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
    this.streamConnectionId = Optional.empty();
  }

  /**
   * Constructs a new runtime exception with {@code null} as its detail message.  The cause is not initialized, and may
   * subsequently be initialized by a call to {@link #initCause}.
   *
   * @param streamConnectionId A {@link StreamConnectionId} that uniquely identifies the Stream connection that emitted
   *                           this exception.
   */
  public StreamException(StreamConnectionId streamConnectionId) {
    this.streamConnectionId = Optional.of(streamConnectionId);
  }

  /**
   * Constructs a new runtime exception with the specified detail message. The cause is not initialized, and may
   * subsequently be initialized by a call to {@link #initCause}.
   *
   * @param message            the detail message. The detail message is saved for later retrieval by the {@link
   *                           #getMessage()} method.
   * @param streamConnectionId A {@link StreamConnectionId} that uniquely identifies the Stream connection that emitted
   *                           this exception.
   */
  public StreamException(String message, StreamConnectionId streamConnectionId) {
    super(message);
    this.streamConnectionId = Optional.of(streamConnectionId);
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
   * @param streamConnectionId A {@link StreamConnectionId} that uniquely identifies the Stream connection that emitted
   *                           this exception.
   *
   * @since 1.4
   */
  public StreamException(String message, Throwable cause,
    StreamConnectionId streamConnectionId) {
    super(message, cause);
    this.streamConnectionId = Optional.of(streamConnectionId);
  }

  /**
   * Constructs a new runtime exception with the specified cause and a detail message of {@code (cause==null ? null :
   * cause.toString())} (which typically contains the class and detail message of {@code cause}).  This constructor is
   * useful for runtime exceptions that are little more than wrappers for other throwables.
   *
   * @param cause              the cause (which is saved for later retrieval by the {@link #getCause()} method).  (A
   *                           {@code null} value is permitted, and indicates that the cause is nonexistent or
   *                           unknown.)
   * @param streamConnectionId A {@link StreamConnectionId} that uniquely identifies the Stream connection that emitted
   *                           this exception.
   *
   * @since 1.4
   */
  public StreamException(Throwable cause, StreamConnectionId streamConnectionId) {
    super(cause);
    this.streamConnectionId = Optional.of(streamConnectionId);
  }

  /**
   * Constructs a new runtime exception with the specified detail message, cause, suppression enabled or disabled, and
   * writable stack trace enabled or disabled.
   *
   * @param message            the detail message.
   * @param cause              the cause.  (A {@code null} value is permitted, and indicates that the cause is
   *                           nonexistent or unknown.)
   * @param enableSuppression  whether or not suppression is enabled or disabled
   * @param writableStackTrace whether or not the stack trace should be writable
   * @param streamConnectionId A {@link StreamConnectionId} that uniquely identifies the Stream connection that emitted
   *                           this exception.
   *
   * @since 1.7
   */
  protected StreamException(String message, Throwable cause, boolean enableSuppression,
    boolean writableStackTrace, StreamConnectionId streamConnectionId) {
    super(message, cause, enableSuppression, writableStackTrace);
    this.streamConnectionId = Optional.of(streamConnectionId);
  }

  /**
   * Returns a short description of this throwable. The result is the concatenation of:
   * <ul>
   * <li> the {@linkplain Class#getName() name} of the class of this object
   * <li> ": " (a colon and a space)
   * <li> the result of invoking this object's {@link #getLocalizedMessage} method
   * <li> the result of invoking the {@link StreamConnectionId#value()} method
   * </ul>
   * If {@code getLocalizedMessage} returns {@code null}, then just
   * the class name is returned.
   *
   * @return a string representation of this throwable.
   */
  public String toString() {
    String str = getClass().getName();
    String message =
      getLocalizedMessage() + " streamConnectionId=" + streamConnectionId.orElse(NO_STREAM_ID);
    return str + ": " + message;
  }

  public StreamConnectionId getStreamConnectionId() {
    return streamConnectionId.orElse(NO_STREAM_ID);
  }
}
