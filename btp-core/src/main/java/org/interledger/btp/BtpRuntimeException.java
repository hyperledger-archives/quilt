package org.interledger.btp;

/*-
 * ========================LICENSE_START=================================
 * Bilateral Transfer Protocol Core Libs
 * %%
 * Copyright (C) 2017 - 2018 Hyperledger and its contributors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */

import org.interledger.core.DateUtils;

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
   * detail message.  The cause is not initialized, and may subsequently be initialized by a call to {@link
   * #initCause}.
   */
  public BtpRuntimeException() {
    super("Unknown BTP Runtime Exception");
    this.code = BtpErrorCode.F00_NotAcceptedError;
    this.triggeredAt = DateUtils.now();
  }

  /**
   * Constructs a new runtime exception with the given code and detail message.  The cause is not initialized, and may
   * subsequently be initialized by a call to {@link #initCause}.
   *
   * @param code A {@link BtpErrorCode} for creating the instance of the exception.
   * @param message An error {@code String} message.
   */
  public BtpRuntimeException(BtpErrorCode code, String message) {
    super(message);
    this.code = code;
    this.triggeredAt = DateUtils.now();
  }

  /**
   * Constructs a new runtime exception with the specified detail message and cause.
   *
   * <p>Note that the detail message associated with {@code cause} is <i>not</i> automatically
   * incorporated in this runtime exception's detail message.
   *
   * @param code A {@link BtpErrorCode} value to create the exception.
   * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
   * @param cause   the cause (which is saved for later retrieval by the {@link #getCause()} method).  (A {@code null}
   *                value is permitted, and indicates that the cause is nonexistent or unknown.)
   */
  public BtpRuntimeException(BtpErrorCode code, String message, Throwable cause) {
    super(message, cause);
    this.code = code;
    this.triggeredAt = DateUtils.now();
  }

  /**
   * Constructs a new runtime exception with the specified cause and with {@code F00 Not Accepted Error} as its error
   * code and a detail message of {@code (cause==null ? null : cause.toString())} (which typically contains the class
   * and detail message of
   * {@code cause}).  This constructor is useful for runtime exceptions that are little more than
   * wrappers for other throwables.
   *
   * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method). (A {@code null}
   *              value is permitted, and indicates that the cause is nonexistent or unknown.)
   */
  public BtpRuntimeException(Throwable cause) {
    super(cause);
    this.code = BtpErrorCode.F00_NotAcceptedError;
    this.triggeredAt = DateUtils.now();
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
   * @param requestId A {@code long} request ID
   *
   * @return a BTP Error message
   */
  public BtpError toBtpError(long requestId) {
    return toBtpError(requestId, new BtpSubProtocols());
  }

  /**
   * Build an error message from the given exception.
   *
   * @param requestId A {@code long} request ID
   * @param subProtocols An instance of {@link BtpSubProtocols}.
   *
   * @return a BTP Error message
   */
  public BtpError toBtpError(long requestId, BtpSubProtocols subProtocols) {

    Objects.requireNonNull(subProtocols, "SubProtocols can be empty but not null.");

    return BtpError.builder()
        .requestId(requestId)
        .errorCode(getCode())
        .triggeredAt(getTriggeredAt())
        .subProtocols(subProtocols)
        .build();
  }

}
