package org.interledger.core;

import org.interledger.annotations.Immutable;

import java.util.Arrays;

public interface InterledgerRejectPacket extends InterledgerPacket {

  /**
   * Get the default builder.
   *
   * @return a {@link InterledgerRejectPacketBuilder} instance.
   */
  static InterledgerRejectPacketBuilder builder() {
    return new InterledgerRejectPacketBuilder();
  }

  /**
   * The Interledger Error Code for this error.
   *
   * @return An {@link InterledgerErrorCode}.
   */
  InterledgerErrorCode getCode();

  /**
   * The {@link InterledgerAddress} of the entity that originally emitted the error.
   *
   * @return An {@link InterledgerAddress}.
   */
  InterledgerAddress getTriggeredBy();

  /**
   * The {@link InterledgerAddress} of the entity that originally emitted the error.
   *
   * @return An {@link InterledgerAddress}.
   */
  String getMessage();

  /**
   * Machine-readable data. The format is defined for each error code. Implementations MUST follow
   *     the correct format for the code given in the `code` field.
   *
   * @return The optional error data.
   */
  byte[] getData();

  @Immutable
  abstract class AbstractInterledgerRejectPacket implements InterledgerRejectPacket {

  }
}
