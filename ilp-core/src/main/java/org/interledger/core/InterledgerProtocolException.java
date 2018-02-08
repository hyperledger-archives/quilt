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
    this.interledgerRejectPacket =
        Objects
            .requireNonNull(interledgerRejectPacket, "interledgerRejectPacket must not be null");
  }

  public InterledgerRejectPacket getInterledgerRejectPacket() {
    return interledgerRejectPacket;
  }

  @Override
  public String toString(){
      return interledgerRejectPacket.toString();
  }

}
