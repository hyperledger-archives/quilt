package org.interledger.node.services;

import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.node.Account;

import java.util.concurrent.Future;

public interface InterledgerRequestHandler {

  /**
   * Asynchronously handle an incoming ILP request.
   *
   * @param sourceAccount the account that the request comes from
   * @param incomingRequest the incoming ILP request
   *
   * @return a future that should resolve to the returned response.
   */
  Future<InterledgerFulfillPacket> handlePacket(
      Account sourceAccount, InterledgerPreparePacket incomingRequest);


  boolean canHandlePacket(Account sourceAccount, InterledgerPreparePacket incomingRequest);
}
