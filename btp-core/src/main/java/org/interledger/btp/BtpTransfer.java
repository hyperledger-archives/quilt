package org.interledger.btp;

import org.interledger.annotations.Immutable;

import java.math.BigInteger;

public interface BtpTransfer extends BtpPacket {

  static BtpTransferBuilder builder() {
    return new BtpTransferBuilder();
  }

  BigInteger getAmount();


  @Immutable
  abstract class AbstractBtpTransfer implements BtpTransfer {

    @Override
    public final BtpMessageType getType() {
      return BtpMessageType.TRANSFER;
    }
  }

}
