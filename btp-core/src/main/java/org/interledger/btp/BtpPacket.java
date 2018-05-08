package org.interledger.btp;

public interface BtpPacket {

  BtpMessageType getType();

  long getRequestId();

  BtpSubProtocols getSubProtocols();

  /**
   * Get the primary {@link BtpSubProtocol}.
   *
   * @return the {@link BtpSubProtocol} that is first in the list
   */
  default BtpSubProtocol getPrimarySubProtocol() {
    if (getSubProtocols().isEmpty()) {
      throw new IndexOutOfBoundsException("No sub-protocols");
    }
    return getSubProtocols().get(0);
  }

  /**
   * Get the {@link BtpSubProtocol} by name.
   *
   * @param protocolName the name of the {@link BtpSubProtocol}
   * @return a {@link BtpSubProtocol} or null if none exists with the given name
   */
  default BtpSubProtocol getSubProtocol(String protocolName) {
    for (BtpSubProtocol protocol : getSubProtocols()) {
      if (protocol.getProtocolName().equals(protocolName)) {
        return protocol;
      }
    }
    return null;
  }

  /**
   * Check if a given {@link BtpSubProtocol} exists in this message.
   *
   * @param protocolName the name of the {@link BtpSubProtocol}
   * @return a <code>true</code> if a {@link BtpSubProtocol} exists with the given name
   */
  default boolean hasSubProtocol(String protocolName) {
    for (BtpSubProtocol protocol : getSubProtocols()) {
      if (protocol.getProtocolName().equals(protocolName)) {
        return true;
      }
    }
    return false;
  }

}
