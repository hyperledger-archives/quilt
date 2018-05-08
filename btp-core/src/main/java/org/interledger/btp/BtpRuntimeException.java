package org.interledger.btp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.time.Instant;
import java.util.Objects;

/**
 * A generic runtime exception that can be thrown when processing BTP messages and later converted into a BTP Error
 * message.
 */
public class BtpRuntimeException extends RuntimeException {

  private final BtpErrorCode code;
  private final Instant triggeredAt;


  /**
   * Constructs a new runtime exception with {@code F99 Application Error} as its error code and an empty string  as
   * detail message.  The cause is not initialized, and may subsequently be initialized by a call to {@link #initCause}.
   */
  public BtpRuntimeException() {
    super("Unknown BTP Runtime Exception");
    this.code = BtpErrorCode.F00_NotAcceptedError;
    this.triggeredAt = Instant.now();
  }

  /**
   * Constructs a new runtime exception with the given code and detail message.  The cause is not
   * initialized, and may subsequently be initialized by a call to {@link #initCause}.
   */
  public BtpRuntimeException(BtpErrorCode code, String message) {
    super(message);
    this.code = code;
    this.triggeredAt = Instant.now();
  }

  /**
   * Constructs a new runtime exception with the specified detail message and cause.
   *
   * <p>Note that the detail message associated with {@code cause} is <i>not</i> automatically incorporated in
   * this runtime exception's detail message.
   *
   * @param message the detail message (which is saved for later retrieval by the {@link
   *                #getMessage()} method).
   * @param cause   the cause (which is saved for later retrieval by the {@link #getCause()}
   *                method).  (A <tt>null</tt> value is permitted, and indicates that the cause is
   *                nonexistent or unknown.)
   */
  public BtpRuntimeException(BtpErrorCode code, String message, Throwable cause) {
    super(message, cause);
    this.code = code;
    this.triggeredAt = Instant.now();
  }

  /**
   * Constructs a new runtime exception with the specified cause and with {@code F00 Not Accepted Error} as its error
   * code and a detail message of <tt>(cause==null ? null : cause.toString())</tt> (which typically contains the
   * class and detail message of <tt>cause</tt>).  This constructor is useful for runtime exceptions that are little
   * more than wrappers for other throwables.
   *
   * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
   *              (A <tt>null</tt> value is permitted, and indicates that the cause is nonexistent
   *              or unknown.)
   */
  public BtpRuntimeException(Throwable cause) {
    super(cause);
    this.code = BtpErrorCode.F00_NotAcceptedError;
    this.triggeredAt = Instant.now();
  }

  public BtpErrorCode getCode() {
    return code;
  }

  public Instant getTriggeredAt() {
    return triggeredAt;
  }

  /**
   * Build an error message from the given exception.
   *
   * @return a BTP Error message
   */
  public BtpError toBtpError(long requestId) {
    return toBtpError(requestId, new BtpSubProtocols());
  }

  /**
   * Build an error message from the given exception.
   *
   * @return a BTP Error message
   */
  public BtpError toBtpError(long requestId, BtpSubProtocols subProtocols) {

    Objects.requireNonNull(subProtocols, "SubProtocols can be empty but not null.");

    byte[] trace;
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
         PrintStream stream = new PrintStream(baos)) {
      printStackTrace(stream);
      stream.flush();
      trace = baos.toByteArray();
    } catch (IOException e) {
      trace = new byte[] {};
    }

    return BtpError.builder()
        .requestId(requestId)
        .errorCode(getCode())
        .errorName(getMessage())
        .errorData(trace)
        .triggeredAt(getTriggeredAt())
        .subProtocols(subProtocols)
        .build();
  }

}