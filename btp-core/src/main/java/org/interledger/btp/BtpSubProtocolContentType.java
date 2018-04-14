package org.interledger.btp;

import static java.lang.String.format;

public enum BtpSubProtocolContentType {
  MIME_APPLICATION_OCTET_STREAM(0),
  MIME_TEXT_PLAIN_UTF8(1),
  MIME_APPLICATION_JSON(2),;

  private final int code;


  BtpSubProtocolContentType(int code) {
    this.code = code;
  }

  public int getCode() {
    return this.code;
  }

  public static BtpSubProtocolContentType fromCode(int code) {

    switch (code) {
      case 0:
        return MIME_APPLICATION_OCTET_STREAM;
      case 1:
        return MIME_TEXT_PLAIN_UTF8;
      case 2:
        return MIME_APPLICATION_JSON;
    }

    throw new IllegalArgumentException(format("Unknown BTP Sub-Protocol Content Type: %s", code));

  }
}
