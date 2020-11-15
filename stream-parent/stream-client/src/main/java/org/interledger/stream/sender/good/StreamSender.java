package org.interledger.stream.sender.good;

import org.interledger.stream.good.SendMoneyRequest;
import org.interledger.stream.good.SendMoneyResult;

import java.util.concurrent.CompletableFuture;

/**
 * Defines a STREAM Client according to IL-RFC-29.
 *
 * @see "https://github.com/interledger/rfcs/blob/master/0029-stream/0029-stream.md"
 */
public interface StreamSender {

  /**
   * <p>Send "money" (i.e., some unit of value) from a source ILP address to a destination address, preventing any
   * unsent STREAM packets from becoming enqueued if the optional payment timeout has been exceeded.</p>
   *
   * <p>Note that, per https://github.com/hyperledger/quilt/issues/242, as of the publication of this client,
   * connectors will reject ILP packets that exceed 32kb (though there is no hard rule that more than 32kb will not be
   * supported in the future.</p>
   *
   * @param request all relevant details about the money to send
   *
   * @return results of the request to send money
   */
  CompletableFuture<SendMoneyResult> sendMoney(SendMoneyRequest request);

}
