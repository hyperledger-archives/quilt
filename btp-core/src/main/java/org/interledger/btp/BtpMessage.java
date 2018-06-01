package org.interledger.btp;

import org.interledger.annotations.Immutable;

public interface BtpMessage extends BtpPacket {

  static BtpMessageBuilder builder() {
    return new BtpMessageBuilder();
  }

  @Immutable
  abstract class AbstractBtpMessage implements BtpMessage {

    @Override
    public final BtpMessageType getType() {
      return BtpMessageType.MESSAGE;
    }

  }

}
