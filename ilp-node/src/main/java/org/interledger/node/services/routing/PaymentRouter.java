package org.interledger.node.services.routing;

import org.interledger.core.InterledgerPreparePacket;
import org.interledger.node.Account;

public interface PaymentRouter {
  Route getBestRoute(Account sourceAccount, InterledgerPreparePacket packet);
}
