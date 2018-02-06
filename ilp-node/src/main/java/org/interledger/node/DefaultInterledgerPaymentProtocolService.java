package org.interledger.node;

import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.node.channels.Channel;
import org.interledger.node.exceptions.InvalidFulfillmentException;
import org.interledger.node.exceptions.RequestRejectedException;
import org.interledger.node.services.AbstractThreadedService;
import org.interledger.node.services.InterledgerPaymentProtocolService;
import org.interledger.node.services.fx.ConversionResult;
import org.interledger.node.services.routing.PaymentRouter;
import org.interledger.node.services.routing.Route;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

public class DefaultInterledgerPaymentProtocolService
    extends AbstractThreadedService
    implements InterledgerPaymentProtocolService {

  final PaymentRouter router;

  public DefaultInterledgerPaymentProtocolService(
      ThreadPoolExecutor pool, NodeConfiguration config, PaymentRouter router) {
    super(pool, config);
    this.router = router;
  }

  @Override
  public Future<InterledgerFulfillPacket> handlePacket(
      final Account sourceAccount, final InterledgerPreparePacket incomingRequest) {

    return submit(() -> {

      //Process the request
      OutgoingRequest outgoing
          = processIncomingRequest(sourceAccount, incomingRequest);

      final Account destinationAccount = outgoing.getAccount();
      final Channel destinationChannel = destinationAccount.getChannel();
      final InterledgerPreparePacket outgoingRequest = outgoing.getRequest();

      //If the channel is not connected then wait and retry
      while (!destinationChannel.isOpen()) {

        //TODO Limit retries

        try {

          //Sleep and then enqueue again
          Thread.sleep(100);

        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new RuntimeException(e);
        }

      }

      try {

        //Block until the channel gets back a response
        InterledgerFulfillPacket incomingResponse
            = destinationChannel.sendRequest(outgoingRequest).get();

        //Synchronously do processing of the response
        return processIncomingResponse(
            sourceAccount, destinationAccount,
            incomingRequest, outgoingRequest,
            incomingResponse);

      } catch (ExecutionException e) {

        if (e.getCause() instanceof RequestRejectedException) {
          //TODO Log error - Rejected upstream
          throw (RequestRejectedException) e.getCause();
        }
        throw e;
      }
    });
  }

  @Override
  public boolean canHandlePacket(Account sourceAccount, InterledgerPreparePacket incomingRequest) {
    //Handle all packets by default
    return true;
  }

  @Override
  public Future<Void> handleTransfer(final Account sourceAccount, final long transferAmount) {
    return submit(() -> {
      //TODO Update balance on account
      return null;
    });
  }

  @Override
  public OutgoingRequest processIncomingRequest(
      Account sourceAccount, InterledgerPreparePacket incomingRequest)
      throws RequestRejectedException {

    //TODO Check balances

    //Get the best route for this packet
    Optional<Route> result = router.findBestNexHop(incomingRequest.getDestination(), sourceAccount);

    //TODO Can't use ifElseThrow due to https://bugs.openjdk.java.net/browse/JDK-8047338
    if(!result.isPresent()) {
      throw new RequestRejectedException(InterledgerRejectPacket.builder()
                .code(InterledgerErrorCode.F02_UNREACHABLE)
                .message("Unable to route message.")
                .triggeredBy(getConfig().getAddress().get())
                .data(new byte[] {})
                .build());
    }

    Route route = result.get();

    //Get the right outgoing amount and expiry
    ConversionResult outgoingAmount = route.getRateConverter().convert(
        incomingRequest.getAmount(),
        incomingRequest.getExpiresAt()
    );

    return new OutgoingRequest(
        route.getDestinationAccount(),
        InterledgerPreparePacket.builder()
            .destination(incomingRequest.getDestination())
            .amount(outgoingAmount.getAmount())
            .expiresAt(outgoingAmount.getExpiry())
            .executionCondition(incomingRequest.getExecutionCondition())
            .data(incomingRequest.getData())
            .build());
  }

  @Override
  public InterledgerFulfillPacket processIncomingResponse(
      Account sourceAccount, Account destinationAccount,
      InterledgerPreparePacket incomingRequest, InterledgerPreparePacket outgoingRequest,
      InterledgerFulfillPacket incomingResponse)
      throws RequestRejectedException {

    //Validate the fulfillment
    if (!incomingResponse.getFulfillment().verify(
        outgoingRequest.getExecutionCondition(), new byte[] {})) {
      throw new InvalidFulfillmentException(outgoingRequest, incomingResponse);
    }

    //TODO Update balances

    //InterledgerFulfillPacket is immutable so we don't need to clone it
    return incomingResponse;
  }

}
