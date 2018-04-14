package org.interledger.btp;

import java.util.ArrayList;

public class BtpSubProtocols extends ArrayList<BtpSubProtocol> {

  //TODO Optimize implementation by indexing by protocol name too

  public static final String AUTH = "auth";
  public static final String AUTH_TOKEN = "auth_token";
  public static final String AUTH_USERNAME = "auth_username";
  public static final String INTERLEDGER = "ilp";

  public BtpSubProtocol getPrimarySubProtocol() {
    return get(0);
  }

  public static BtpSubProtocols fromPrimarySubProtocol(BtpSubProtocol protocol) {
    BtpSubProtocols subProtocols = new BtpSubProtocols();
    subProtocols.add(protocol);
    return subProtocols;
  }


  public boolean hasSubProtocol(String protocolName) {
    for (BtpSubProtocol protocol : this) {
      if (protocol.getProtocolName().equals(protocolName)) {
        return true;
      }
    }
    return false;
  }

  public BtpSubProtocol getSubProtocol(String protocolName) {
    for (BtpSubProtocol protocol : this) {
      if (protocol.getProtocolName().equals(protocolName)) {
        return protocol;
      }
    }
    return null;
  }

}
