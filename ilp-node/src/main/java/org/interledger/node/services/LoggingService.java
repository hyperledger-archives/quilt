package org.interledger.node.services;

import org.interledger.core.InterledgerPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.node.Account;
import org.interledger.node.events.Event;

public interface LoggingService {

  void logEvent(Account source, Event event);
  void logPacketReceived(Account source, InterledgerPacket packet);
  void logPacketSent(Account source, InterledgerPacket packet);

}
