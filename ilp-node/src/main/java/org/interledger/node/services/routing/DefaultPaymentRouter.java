package org.interledger.node.services.routing;

import org.interledger.core.InterledgerPreparePacket;
import org.interledger.node.Account;

public class DefaultPaymentRouter implements PaymentRouter {

  @Override
  public Route getBestRoute(Account sourceAccount, InterledgerPreparePacket packet) {
    throw new UnsupportedOperationException("Not implemented.");
  }

}
