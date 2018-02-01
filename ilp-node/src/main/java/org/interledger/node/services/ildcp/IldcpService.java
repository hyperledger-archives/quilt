package org.interledger.node.services.ildcp;

import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.node.Account;
import org.interledger.node.exceptions.RequestRejectedException;
import org.interledger.node.services.InterledgerPeerProtocolService;

import java.util.Base64;
import java.util.concurrent.Future;

public interface IldcpService
    extends InterledgerPeerProtocolService {

  String ILDCP_DESTINATION = "peer.ildcp";
  String BALANCE_QUERY_DESTINATION = "peer.balance";

  /**
   * Asynchronously send an ILDCP request to the parent.
   *
   * @param parent the account of the parrent node to send the request to
   *
   * @return a future that will complete after {@link #processIncomingConfigResponse(Account,
   * InterledgerFulfillPacket)} has been called on the response.
   */
  Future<Void> requestConfigurationFromParent(Account parent);

  /**
   * Process an ILDCP request from a child node and construct a response.
   *
   * @param childAccount the requesting account
   * @param incomingRequest the incoming request
   * @return
   * @throws RequestRejectedException
   */
  InterledgerFulfillPacket processIncomingConfigRequest(
      Account childAccount, InterledgerPreparePacket incomingRequest) throws RequestRejectedException;

  /**
   * Process an ILDCP response from a parent.
   *
   * @param parent the responding account
   * @param response the incoming response
   * @return
   */
  void processIncomingConfigResponse(Account parent, InterledgerFulfillPacket response);

}
