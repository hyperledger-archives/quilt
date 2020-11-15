package org.interledger.stream.pay.model;

import java.util.concurrent.CompletableFuture;

/**
 * (Serialize & send) and (receive & authenticate) all ILP and STREAM packets.
 *
 * @deprecated use the other StreamConnection instead.
 */
@Deprecated
public interface StreamConnection {

  /**
   * Send packets as frequently as controllers will allow, until they end the send loop
   */
  CompletableFuture<SendState> runSendLoop();

  /**
   * Send an ILP Prepare over STREAM, then parse and validate the reply
   */
  CompletableFuture<StreamPacketReply> sendRequest(StreamPacketRequest streamPacketRequest);


}
