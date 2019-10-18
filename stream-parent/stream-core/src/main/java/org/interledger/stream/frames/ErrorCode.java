package org.interledger.stream.frames;

import org.interledger.core.Immutable;

/**
 * Defines an error code for the purposes of the STREAM protocol. Error codes are are sent in {@link StreamCloseFrame}
 * and {@link ConnectionCloseFrame} to indicate what caused the stream or connection to be closed.
 */
@Immutable
public interface ErrorCode {

  static ErrorCodeBuilder builder() {
    return new ErrorCodeBuilder();
  }

  /**
   * The STREAM error code.
   *
   * @return A {@link short} representing the error code.
   */
  short code();

  /**
   * The description of the error corresponding to {@link #code()}.
   *
   * @return A {@link String} containing a description of this error code.
   */
  String description();

}
