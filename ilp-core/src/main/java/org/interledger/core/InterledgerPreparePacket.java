package org.interledger.core;

import org.interledger.annotations.Immutable;
import org.interledger.cryptoconditions.PreimageSha256Condition;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Arrays;

/**
 * <p>Interledger Payments moves assets of one party to another that consists of one or more ledger
 * transfers, potentially across multiple ledgers.</p>
 *
 * <p>Interledger Payments have three major consumers:</p>
 *   <ul>
 *     <li>Connectors utilize the Interledger Address contained in the payment to route the
 * payment.</li>
 *     <li>The receiver of a payment uses it to identify the recipient and which condition to
 * fulfill.</li>
 *     <li>Interledger sub-protocols utilize custom data encoded in a payment to facilitate
 * sub-protocol operations.</li>
 *   </ul>
 *
 * <p>When a sender prepares a transfer to start a payment, the sender attaches an ILP Payment to
 * the transfer, in the memo field if possible. If a ledger does not support attaching the entire
 * ILP Payment to a transfer as a memo, users of that ledger can transmit the ILP Payment using
 * another authenticated messaging channel, but MUST be able to correlate transfers and ILP
 * Payments.</p>
 *
 * <p>When a connector sees an incoming prepared transfer with an ILP Payment, the receiver reads
 * the ILP Payment to confirm the details of the packet. For example, the connector reads the
 * InterledgerAddress of the payment's receiver, and if the connector has a route to the receiver's
 * account, the connector prepares a transfer to continue the payment, and attaches the same ILP
 * Payment to the new transfer. Likewise, the receiver confirms that the amount of the ILP Payment
 * Packet matches the amount actually delivered by the transfer. And finally, the receiver decodes
 * the data portion of the Payment and matches the condition to the payment.</p>
 *
 * <p>The receiver MUST confirm the integrity of the ILP Payment, for example with a hash-based
 * message authentication code (HMAC). If the receiver finds the transfer acceptable, the receiver
 * releases the fulfillment for the transfer, which can be used to execute all prepared transfers
 * that were established prior to the receiver accepting the payment.</p>
 */
public interface InterledgerPreparePacket extends InterledgerPacket {

  /**
   * Get the default builder.
   *
   * @return a {@link InterledgerPreparePacketBuilder} instance.
   */
  static InterledgerPreparePacketBuilder builder() {
    return new InterledgerPreparePacketBuilder();
  }

  BigInteger getAmount();

  Instant getExpiresAt();

  PreimageSha256Condition getExecutionCondition();

  /**
   * The Interledger address of the account where the receiver should ultimately receive the
   * payment.
   *
   * @return An instance of {@link InterledgerAddress}.
   */
  InterledgerAddress getDestination();

  /**
   * Arbitrary data for the receiver that is set by the transport layer of a payment (for example,
   * this may contain PSK data).
   *
   * @return A byte array.
   */
  byte[] getData();

  @Immutable
  abstract class AbstractInterledgerPreparePacket implements InterledgerPreparePacket {

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }

      InterledgerPreparePacket impl = (InterledgerPreparePacket) obj;

      return getAmount().equals(impl.getAmount())
          && getExpiresAt().equals(impl.getExpiresAt())
          && getExecutionCondition().equals(impl.getExecutionCondition())
          && getDestination().equals(impl.getDestination())
          && Arrays.equals(getData(), impl.getData());
    }

    @Override
    public int hashCode() {
      int result = getAmount().hashCode();
      result = 31 * result + getExpiresAt().hashCode();
      result = 31 * result + getExecutionCondition().hashCode();
      result = 31 * result + getDestination().hashCode();
      result = 31 * result + Arrays.hashCode(getData());
      return result;
    }

    @Override
    public String toString() {
      return "InterledgerPreparePacket{"
          + ", amount=" + getAmount().toString(10)
          + ", expiresAt=" + getExpiresAt().toString()
          + ", executionCondition=" + getExecutionCondition()
          + ", destination=" + getDestination()
          + ", data=" + Arrays.toString(getData())
          + '}';
    }
  }

}