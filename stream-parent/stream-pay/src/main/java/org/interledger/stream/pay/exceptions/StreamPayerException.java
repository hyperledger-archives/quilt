package org.interledger.stream.pay.exceptions;

import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.StreamException;

import java.util.Objects;

public class StreamPayerException extends StreamException {

  private final SendState sendState;

  /**
   * Constructs a new runtime exception with {@code null} as its detail message.  The cause is not initialized, and may
   * subsequently be initialized by a call to {@link #initCause}.
   *
   * @param sendState
   */
  public StreamPayerException(SendState sendState) {
    super();
    this.sendState = sendState;
  }

  /**
   * Constructs a new runtime exception with the specified detail message. The cause is not initialized, and may
   * subsequently be initialized by a call to {@link #initCause}.
   *
   * @param message   the detail message. The detail message is saved for later retrieval by the {@link #getMessage()}
   *                  method.
   * @param sendState
   */
  public StreamPayerException(String message, SendState sendState) {
    super(message);
    this.sendState = sendState;
  }

  /**
   * Constructs a new runtime exception with the specified detail message and cause.  <p>Note that the detail message
   * associated with {@code cause} is <i>not</i> automatically incorporated in this runtime exception's detail message.
   * </p>
   *
   * @param message   the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
   * @param cause     the cause (which is saved for later retrieval by the {@link #getCause()} method).  (A {@code null}
   *                  value is permitted, and indicates that the cause is nonexistent or unknown.)
   * @param sendState
   * @since 1.4
   */
  public StreamPayerException(String message, Throwable cause, SendState sendState) {
    super(message, cause);
    this.sendState = sendState;
  }

  /**
   * Constructs a new runtime exception with the specified cause and a detail message of {@code (cause==null ? null :
   * cause.toString())} (which typically contains the class and detail message of {@code cause}).  This constructor is
   * useful for runtime exceptions that are little more than wrappers for other throwables.
   *
   * @param cause     the cause (which is saved for later retrieval by the {@link #getCause()} method).  (A {@code null}
   *                  value is permitted, and indicates that the cause is nonexistent or unknown.)
   * @param sendState
   * @since 1.4
   */
  public StreamPayerException(Throwable cause, SendState sendState) {
    super(cause);
    this.sendState = sendState;
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
   * @param sendState
   * @since 1.7
   */
  protected StreamPayerException(String message, Throwable cause, boolean enableSuppression,
    boolean writableStackTrace, SendState sendState) {
    super(message, cause, enableSuppression, writableStackTrace);
    this.sendState = sendState;
  }

  public SendState getSendState() {
    return sendState;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StreamPayerException that = (StreamPayerException) o;
    return sendState == that.sendState;
  }

  @Override
  public int hashCode() {
    return Objects.hash(sendState);
  }

  @Override
  public String toString() {
    String message = getLocalizedMessage();
    String output = getClass().getName() + ": " + message + " (sendState=" + sendState + ")";
    return output;
  }
}
