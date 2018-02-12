package org.interledger.node;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerRuntimeException;
import org.interledger.node.exceptions.RequestRejectedException;
import org.interledger.node.services.AbstractThreadedService;
import org.interledger.node.services.ildcp.IldcpResponse;
import org.interledger.node.services.ildcp.IldcpService;

import java.math.BigInteger;
import java.time.Instant;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

public class DefaultInterledgerPeerProtocolService
    extends AbstractThreadedService
    implements IldcpService {

  private final NodeConfiguration config;

  public DefaultInterledgerPeerProtocolService(ThreadPoolExecutor pool, NodeConfiguration config) {
    super(pool, config);
    this.config = config;
  }

  @Override
  public Future<InterledgerFulfillPacket> handlePacket(
      Account requestingAccount, InterledgerPreparePacket request) {

    if (PEER_PROTOCOL_CONDITION.equals(request.getExecutionCondition())) {
      throw new InterledgerRuntimeException("Invalid condition for a peer protocol request");
    }

    return submit(() -> {

      if (request.getDestination().equals(ILDCP_DESTINATION)) {
        return processIncomingConfigRequest(requestingAccount, request);
      }

      if (request.getDestination().equals(BALANCE_QUERY_DESTINATION)) {

        //TODO Handle balance enquiry
        throw new RuntimeException("Balance protocol Not implemented.");
      }

      throw new InterledgerRuntimeException("Unrecognized peer protocol packet");

    });
  }

  @Override
  public Future<Void> requestConfigurationFromParent(Account parent) {

    return submit(() -> {

      //Block on getting the response
      InterledgerFulfillPacket response = parent.getChannel().sendRequest(
          InterledgerPreparePacket.builder()
              .amount(BigInteger.ZERO)
              .destination(InterledgerAddress.of(ILDCP_DESTINATION))
              .executionCondition(PEER_PROTOCOL_CONDITION)
              .expiresAt(Instant.now().plusSeconds(30))
              .data(new byte[] {})
              .build()).get();

      //Apply the ildcp
      processIncomingConfigResponse(parent, response);

      return null;

    });

  }

  @Override
  public InterledgerFulfillPacket processIncomingConfigRequest(
      Account child, InterledgerPreparePacket request) throws RequestRejectedException {

    if (!(child.getRelationship() == AccountRelationship.CHILD)) {
      throw new RequestRejectedException(
          InterledgerRejectPacket.builder()
              .message("Can only provide ildcp for child nodes.")
              .code(InterledgerErrorCode.F00_BAD_REQUEST)
              .triggeredBy(config.getAddress().get())
              .data(new byte[] {})
              .build()
      );
    }

    IldcpResponse response =
        IldcpResponse.builder()
            .interledgerAddress(getInterledgerAddress(child))
            .currencyUnit(child.getCurrencyUnit())
            .currencyScale(child.getCurrencyScale())
            .build();

    return InterledgerFulfillPacket.builder()
        .fulfillment(PEER_PROTOCOL_FULFILLMENT)
        .data(
            /* TODO ILDCP Encoder (ASN.1 and OER)
              From JS implementation:
                writer.writeVarOctetString(Buffer.from(response.clientAddress, 'ascii'))
                writer.writeUInt8(response.assetScale)
                writer.writeVarOctetString(Buffer.from(response.assetCode, 'utf8'))
            */ new byte[] {})
        .build();
  }

  @Override
  public void processIncomingConfigResponse(
      Account parent, InterledgerFulfillPacket response) {

    //TODO Decode data from packet into response and update ildcp
  }

  private InterledgerAddress getInterledgerAddress(Account child) {
    //TODO Build address from configurable value not just account id
    return config.getAddress().get().with(child.getAccountId().valueString());
  }


}
