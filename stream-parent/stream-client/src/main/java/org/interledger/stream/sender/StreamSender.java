package org.interledger.stream.sender;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.SharedSecret;
import org.interledger.stream.SendMoneyResult;

import com.google.common.primitives.UnsignedLong;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Defines a STREAM Client according to IL-RFC-29.
 *
 * @see "https://github.com/interledger/rfcs/blob/master/0029-stream/0029-stream.md"
 */
public interface StreamSender {

  /**
   * <p>Send "money" (i.e., some unit of value) from a source ILP address to a destination address.</p>
   *
   * <p>Note that, per https://github.com/hyperledger/quilt/issues/242, as of the publication of this client,
   * connectors will reject ILP packets that exceed 32kb (though there is no hard rule that more than 32kb will not be
   * supported in the future.</p>
   *
   * @param sharedSecret       A {@link SharedSecret} known only to the sender and receiver, negotiated using some
   *                           higher-level protocol (e.g., SPSP or something else).
   * @param sourceAddress      The {@link InterledgerAddress} of the source of this payment.
   * @param destinationAddress The {@link InterledgerAddress} of the receiver of this payment.
   * @param amount             An {@link UnsignedLong} containing the amount of this payment.
   *
   * @return A {@link CompletableFuture} that, once complete, will contain either an error or an instance of {@link
   *     SendMoneyResult} that displays the result of this "send money" request.
   */
  CompletableFuture<SendMoneyResult> sendMoney(
      final SharedSecret sharedSecret,
      final InterledgerAddress sourceAddress,
      final InterledgerAddress destinationAddress,
      final UnsignedLong amount
  );

  /**
   * <p>Send "money" (i.e., some unit of value) from a source ILP address to a destination address, preventing any
   * unsent STREAM packets from becoming encqueed if the payment timeout has been exceeded.</p>
   *
   * <p>Note that, per https://github.com/hyperledger/quilt/issues/242, as of the publication of this client,
   * connectors will reject ILP packets that exceed 32kb (though there is no hard rule that more than 32kb will not be
   * supported in the future.</p>
   *
   * @param sharedSecret       A {@link SharedSecret} known only to the sender and receiver, negotiated using some
   *                           higher-level protocol (e.g., SPSP or something else).
   * @param sourceAddress      The {@link InterledgerAddress} of the source of this payment.
   * @param destinationAddress The {@link InterledgerAddress} of the receiver of this payment.
   * @param amount             An {@link UnsignedLong} containing the amount of this payment.
   * @param timeout            A {@link Duration} to wait before no longer scheduling any off more requests to send
   *                           stream packets for this payment. This is an important distinction as compared to a
   *                           traditional `hard timeout` where processing might end immediately upon timeout. Instead,
   *                           this timeout should be considered to be a `soft timeout`, because the sender will wait
   *                           for any in-flight packetized payments to be sent before exiting out of it run-loop.
   *
   * @return A {@link CompletableFuture} that, once complete, will contain either an error or an instance of {@link
   *     SendMoneyResult} that displays the result of this "send money" request.
   */
  CompletableFuture<SendMoneyResult> sendMoney(
      final SharedSecret sharedSecret,
      final InterledgerAddress sourceAddress,
      final InterledgerAddress destinationAddress,
      final UnsignedLong amount,
      final Duration timeout
  );

}
