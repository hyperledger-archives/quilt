package org.interledger.node.services;

import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.node.Account;
import org.interledger.node.exceptions.RequestRejectedException;
import org.interledger.node.services.InterledgerRequestHandler;

import java.util.concurrent.Future;

public interface InterledgerPaymentProtocolService extends InterledgerRequestHandler {

  /**
   * Asynchronously handle an incoming transfer.
   *
   * <p><strong>WARNING:</strong>The amount is an unsigned long, i.e. UInt64.
   *
   * @param sourceAccount the account that the transfer comes from
   * @param transferAmount the amount of the transfer (unsigned long)
   *
   * @return
   */
  Future<Void> handleTransfer(Account sourceAccount, long transferAmount);

  /**
   * Process an ILP request and determine the request to forward and the account to forward to
   *
   * <p>Implementations should perform all necessary checks such as verifying the liquidity of the
   * source account.
   *
   * @param sourceAccount originator of the ILP request
   * @param incomingRequest the incoming request
   * @return
   * @throws RequestRejectedException if the request
   */
  OutgoingRequest processIncomingRequest(
      Account sourceAccount, InterledgerPreparePacket incomingRequest)
      throws RequestRejectedException;

  /**
   * Process an ILP response
   *
   * @param sourceAccount originator of the ILP request
   * @param destinationAccount peer to which the request was sent
   * @param incomingRequest the original incoming request
   * @param outgoingRequest the original outgoing request
   * @param incomingResponse incoming response
   * @return the response to forward on to the original requester
   * @throws RequestRejectedException
   */
  InterledgerFulfillPacket processIncomingResponse(
      Account sourceAccount, Account destinationAccount,
      InterledgerPreparePacket incomingRequest, InterledgerPreparePacket outgoingRequest,
      InterledgerFulfillPacket incomingResponse)
      throws RequestRejectedException;

  /**
   * Data structure for complex return type
   */
  class OutgoingRequest {

    private Account account;
    private InterledgerPreparePacket request;

    public OutgoingRequest(Account account, InterledgerPreparePacket request) {
      this.account = account;
      this.request = request;
    }

    public Account getAccount() {
      return account;
    }

    public InterledgerPreparePacket getRequest() {
      return request;
    }

  }

}
