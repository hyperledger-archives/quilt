package org.interledger.btp;

import org.interledger.annotations.Immutable;

public interface BtpResponse extends BtpPacket {

  static BtpResponseBuilder builder() {
    return new BtpResponseBuilder();
  }

  @Immutable
  abstract class AbstractBtpResponse implements BtpResponse {

    @Override
    public final BtpMessageType getType() {
      return BtpMessageType.RESPONSE;
    }

  }

}
