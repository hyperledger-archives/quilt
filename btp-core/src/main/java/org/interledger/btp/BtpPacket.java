package org.interledger.btp;

public interface BtpPacket {

  BtpMessageType getType();

  long getRequestId();

  BtpSubProtocols getSubProtocols();

  default BtpSubProtocol getPrimarySubProtocol() {
    if (getSubProtocols().isEmpty()) {
      throw new IndexOutOfBoundsException("No sub-protocols");
    }
    return getSubProtocols().get(0);
  }

  default BtpSubProtocol getSubProtocol(String protocolName) {
    for (BtpSubProtocol protocol : getSubProtocols()) {
      if (protocol.getProtocolName().equals(protocolName)) {
        return protocol;
      }
    }
    return null;
  }

  default boolean hasSubProtocol(String protocolName) {
    for (BtpSubProtocol protocol : getSubProtocols()) {
      if (protocol.getProtocolName().equals(protocolName)) {
        return true;
      }
    }
    return false;
  };

}
