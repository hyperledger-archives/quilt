package org.interledger.core;

import static java.lang.String.format;

/**
 * Per the IL-RFC-27, ILPv4 packets have three types: 12 for Prepare, 13 for Fulfill, and 14 for Reject. This enum
 * provides some type safety for code that might need this information.
 *
 * @see "https://github.com/interledger/rfcs/tree/master/0027-interledger-protocol-4"
 */
public enum InterledgerPacketType {

  PREPARE((short) 12),
  FULFILL((short) 13),
  REJECT((short) 14);

  public static final short PREPARE_CODE = (short) 12;
  public static final short FULFILL_CODE = (short) 13;
  public static final short REJECT_CODE = (short) 14;

  private final short type;

  InterledgerPacketType(short type) {
    this.type = type;
  }

  /**
   * Get a new {@link InterledgerPacketType} from the type.
   *
   * @param code the message type type.
   *
   * @return A new {@link InterledgerPacketType} from the provided type
   */
  public static InterledgerPacketType fromCode(short code) {
    switch (code) {
      case PREPARE_CODE:
        return InterledgerPacketType.PREPARE;
      case FULFILL_CODE:
        return InterledgerPacketType.FULFILL;
      case REJECT_CODE:
        return InterledgerPacketType.REJECT;
      default:
        throw new IllegalArgumentException(format("Unknown StreamPacketType: %s", code));
    }
  }

  public short getType() {
    return this.type;
  }
}
