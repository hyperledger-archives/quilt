package org.interledger.core;

import java.util.Objects;

/**
 * Base ILP exception, see RFC REF: https://interledger.org/rfcs/0003-interledger-protocol/#errors
 */
public class InterledgerProtocolException extends InterledgerRuntimeException {

  private static final long serialVersionUID = 1L;

  private final InterledgerRejectPacket interledgerRejectPacket;

  /**
   * Required-args constructor.
   *
   * @param interledgerRejectPacket An instance of {@link InterledgerRejectPacket} that is the
   *                                 underlying error encapsulated by this exception.
   */
  public InterledgerProtocolException(final InterledgerRejectPacket interledgerRejectPacket) {
    super("Interledger Rejection.");
    this.interledgerRejectPacket = fillInterledgerRejectPacket(interledgerRejectPacket);
  }

  /**
   * Constructs a new Interledger protocol exception with the specified reject packet and
   * detail message.
   *
   * @param interledgerRejectPacket An instance of {@link InterledgerRejectPacket} that is the
   *                                 underlying error encapsulated by this exception.
   * @param message The detail message.
   */
  public InterledgerProtocolException(final InterledgerRejectPacket interledgerRejectPacket,
      final String message) {
    super(message);
    this.interledgerRejectPacket = fillInterledgerRejectPacket(interledgerRejectPacket);
  }

  public InterledgerRejectPacket getInterledgerRejectPacket() {
    return interledgerRejectPacket;
  }

  private InterledgerRejectPacket fillInterledgerRejectPacket(
      final InterledgerRejectPacket interledgerRejectPacket
  ) {
    return Objects
        .requireNonNull(interledgerRejectPacket, "interledgerRejectPacket must not be null");
  }
}
