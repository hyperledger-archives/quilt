package org.interledger.btp;

import static java.lang.String.format;

public enum BtpMessageType {
  RESPONSE(1),
  ERROR(2),
  MESSAGE(6),
  TRANSFER(7);


  private final int code;

  BtpMessageType(int code) {
    this.code = code;
  }

  public int getCode() {
    return this.code;
  }

  public static BtpMessageType fromCode(int code) {

    switch (code) {
      case 1:
        return BtpMessageType.RESPONSE;
      case 2:
        return BtpMessageType.ERROR;
      case 6:
        return BtpMessageType.MESSAGE;
      case 7:
        return BtpMessageType.TRANSFER;
    }

    throw new IllegalArgumentException(format("Unknown BTP Message Type: %s", code));

  }
}

