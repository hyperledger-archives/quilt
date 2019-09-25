package org.interledger.stream.sender;

import org.interledger.core.InterledgerAddress;
import org.interledger.stream.SendMoneyResult;

import com.google.common.primitives.UnsignedLong;

import java.util.concurrent.CompletableFuture;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Defines a STREAM Client according to IL-RFC-29.
 *
 * @see "https://github.com/interledger/rfcs/blob/master/0029-stream/0029-stream.md"
 */
@ThreadSafe
public interface StreamSender {

  /**
   * Send "money" (i.e., some unit of value) from a source ILP address to a destination address.
   *
   * Note that, per https://github.com/hyperledger/quilt/issues/242, as of the publication of this client, connectors
   * will reject ILP packets that exceed 32kb (though there is no hard rule that more than 32kb will not be supported
   * in the future.
   *
   * @param sharedSecret       A shared-secret known only to the sender and receiver, negotiated using some higher-level
   *                           protocol (e.g., SPSP).
   * @param sourceAddress      The {@link InterledgerAddress} of the source of this payment.
   * @param destinationAddress The {@link InterledgerAddress} of the receiver of this payment.
   * @param amount             A {@link UnsignedLong} containing the amount of this payment.
   *
   * @return A {@link CompletableFuture} that, once complete, will contain either an error or an instance of {@link
   *     SendMoneyResult} that displays the result of this "send money" request.
   */
  CompletableFuture<SendMoneyResult> sendMoney(
      final byte[] sharedSecret,
      final InterledgerAddress sourceAddress,
      final InterledgerAddress destinationAddress,
      final UnsignedLong amount
  );

}
