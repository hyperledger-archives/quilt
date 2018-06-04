package org.interledger.btp;

import org.interledger.annotations.Immutable;

import java.nio.charset.StandardCharsets;

public interface BtpSubProtocol {

  static BtpSubProtocolBuilder builder() {
    return new BtpSubProtocolBuilder();
  }

  String getProtocolName();

  BtpSubProtocolContentType getContentType();

  byte[] getData();

  default String getDataAsString() {
    return new String(getData(), StandardCharsets.UTF_8);
  }

  @Immutable
  abstract class AbstractBtpSubProtocol implements BtpSubProtocol {

  }

}
