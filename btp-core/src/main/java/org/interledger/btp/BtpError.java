package org.interledger.btp;

import org.interledger.annotations.Immutable;

import java.time.Instant;

public interface BtpError extends BtpPacket {

  static BtpErrorBuilder builder() {
    return new BtpErrorBuilder();
  }

  BtpErrorCode getErrorCode();

  String getErrorName();

  Instant getTriggeredAt();

  byte[] getErrorData();

  @Immutable
  abstract class AbstractBtpError implements BtpError {

    @Override
    public final BtpMessageType getType() {
      return BtpMessageType.ERROR;
    }

  }

}
