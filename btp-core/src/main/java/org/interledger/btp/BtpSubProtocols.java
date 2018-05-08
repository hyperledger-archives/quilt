package org.interledger.btp;

import java.util.ArrayList;

public class BtpSubProtocols extends ArrayList<BtpSubProtocol> {

  //TODO Optimize implementation by indexing by protocol name too

  public static final String AUTH = "auth";
  public static final String AUTH_TOKEN = "auth_token";
  public static final String AUTH_USERNAME = "auth_username";
  public static final String INTERLEDGER = "ilp";

  /**
   * Get the primary {@link BtpSubProtocol}.
   *
   * @return the {@link BtpSubProtocol} that is first in the list
   */
  public BtpSubProtocol getPrimarySubProtocol() {
    return get(0);
  }

  /**
   * Create a new {@link BtpSubProtocols} list with the given {@link BtpSubProtocol} as the primary sub-protocol.
   * @param protocol the sub-protocol to use as the primary
   * @return a new {@link BtpSubProtocols} list with only a primary sub-protocol
   */
  public static BtpSubProtocols fromPrimarySubProtocol(BtpSubProtocol protocol) {
    BtpSubProtocols subProtocols = new BtpSubProtocols();
    subProtocols.add(protocol);
    return subProtocols;
  }


  /**
   * Check if a given {@link BtpSubProtocol} exists in this list.
   *
   * @param protocolName the name of the {@link BtpSubProtocol}
   * @return a <code>true</code> if a {@link BtpSubProtocol} exists with the given name
   */
  public boolean hasSubProtocol(String protocolName) {
    for (BtpSubProtocol protocol : this) {
      if (protocol.getProtocolName().equals(protocolName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Get the {@link BtpSubProtocol} by name.
   *
   * @param protocolName the name of the {@link BtpSubProtocol}
   * @return a {@link BtpSubProtocol} or null if none exists with the given name
   */
  public BtpSubProtocol getSubProtocol(String protocolName) {
    for (BtpSubProtocol protocol : this) {
      if (protocol.getProtocolName().equals(protocolName)) {
        return protocol;
      }
    }
    return null;
  }

}
