package org.interledger.node.services;

import org.interledger.core.InterledgerPreparePacket;
import org.interledger.cryptoconditions.PreimageSha256Condition;
import org.interledger.cryptoconditions.PreimageSha256Fulfillment;
import org.interledger.node.Account;

public interface InterledgerPeerProtocolService extends InterledgerRequestHandler {

  String PEER_PROTOCOL_ADDRESS_PREFIX = "peer.";
  byte[] PEER_PROTOCOL_FULFILLMENT_BYTES = new byte[32];

  PreimageSha256Fulfillment PEER_PROTOCOL_FULFILLMENT
                                  = PreimageSha256Fulfillment.from(PEER_PROTOCOL_FULFILLMENT_BYTES);
  PreimageSha256Condition PEER_PROTOCOL_CONDITION
                                  = PEER_PROTOCOL_FULFILLMENT.getCondition();

  @Override
  default boolean canHandlePacket(Account sourceAccount, InterledgerPreparePacket incomingRequest) {
    return incomingRequest.getDestination().startsWith(PEER_PROTOCOL_ADDRESS_PREFIX);
  }
}
