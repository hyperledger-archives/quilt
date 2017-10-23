package org.interledger;

import org.interledger.ilp.InterledgerProtocolError;

import java.util.Objects;

/**
 * Base ILP exception, see RFC REF: https://interledger.org/rfcs/0003-interledger-protocol/#errors
 */
public class InterledgerProtocolException extends InterledgerRuntimeException {

  private static final long serialVersionUID = 1L;

  private final InterledgerProtocolError interledgerProtocolError;

  /**
   * Required-args constructor.
   *
   * @param interledgerProtocolError An instance of {@link InterledgerProtocolError} that is the
   *                                 underlying error encapsulated by this exception.
   */
  public InterledgerProtocolException(final InterledgerProtocolError interledgerProtocolError) {
    super("Interledger protocol error.");
    this.interledgerProtocolError =
      Objects
        .requireNonNull(interledgerProtocolError, "interledgerProtocolError must not be null");
  }

  public InterledgerProtocolError getInterledgerProtocolError() {
    return interledgerProtocolError;
  }
}
