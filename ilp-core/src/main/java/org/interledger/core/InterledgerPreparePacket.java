package org.interledger.core;

/*-
 * ========================LICENSE_START=================================
 * Interledger Core
 * %%
 * Copyright (C) 2017 - 2018 Hyperledger and its contributors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */

import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value.Default;

import java.time.Instant;
import java.util.Base64;

/**
 * <p>Interledger Payments moves assets of one party to another that consists of one or more ledger
 * transfers, potentially across multiple ledgers.</p>
 *
 * <p>Interledger Payments have three major consumers:</p>
 * <ul>
 * <li>Connectors utilize the Interledger Address contained in the payment to route the
 * payment.</li>
 * <li>The receiver of a payment uses it to identify the recipient and which condition to
 * fulfill.</li>
 * <li>Interledger sub-protocols utilize custom data encoded in a payment to facilitate
 * sub-protocol operations.</li>
 * </ul>
 *
 * <p>When a sender prepares a transfer to start a payment, the sender attaches an ILP Payment to
 * the transfer, in the memo field if possible. If a ledger does not support attaching the entire ILP Payment to a
 * transfer as a memo, users of that ledger can transmit the ILP Payment using another authenticated messaging channel,
 * but MUST be able to correlate transfers and ILP Payments.</p>
 *
 * <p>When a connector sees an incoming prepared transfer with an ILP Payment, the receiver reads
 * the ILP Payment to confirm the details of the packet. For example, the connector reads the InterledgerAddress of the
 * payment's receiver, and if the connector has a route to the receiver's account, the connector prepares a transfer to
 * continue the payment, and attaches the same ILP Payment to the new transfer. Likewise, the receiver confirms that the
 * amount of the ILP Payment Packet matches the amount actually delivered by the transfer. And finally, the receiver
 * decodes the data portion of the Payment and matches the condition to the payment.</p>
 *
 * <p>The receiver MUST confirm the integrity of the ILP Payment, for example with a hash-based
 * message authentication code (HMAC). If the receiver finds the transfer acceptable, the receiver releases the
 * fulfillment for the transfer, which can be used to execute all prepared transfers that were established prior to the
 * receiver accepting the payment.</p>
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

  /**
   * Local amount, denominated in the minimum divisible unit of the asset of the bilateral relationship. This field is
   * modified by each connector, who applies their exchange rate and adjusts the amount to the appropriate scale and
   * precision of the outgoing account
   *
   * @return A {@link UnsignedLong} representing the amount of this packet.
   */
  @Default
  default UnsignedLong getAmount() {
    return UnsignedLong.ZERO;
  }

  /**
   * The Date and time when the packet expires. Each connector changes the value of this field to set the expiry to an
   * earlier time, before forwarding the packet.
   *
   * @return The {@link Instant} this packet should be considered to be expired.
   */
  Instant getExpiresAt();

  /**
   * SHA-256 hash digest of the fulfillment that will execute the transfer of value represented by this packet.
   * Connectors MUST NOT modify this field. The receiver must be able to fulfill this condition to receive the money.
   *
   * @return A {@link InterledgerCondition} to be fulfilled or rejected.
   */
  InterledgerCondition getExecutionCondition();

  /**
   * The Interledger address of the receiver of this packet.
   *
   * @return An instance of {@link InterledgerAddress}.
   */
  InterledgerAddress getDestination();

  /**
   * End-to-end data. Connectors MUST NOT modify this data. Most higher-level protocols will encrypt and authenticate
   * this data, so receivers will reject packets in which the data is modified
   *
   * @return A byte array of data.
   */
  byte[] getData();

  @Immutable
  abstract class AbstractInterledgerPreparePacket implements InterledgerPreparePacket {

    @Override
    @Default
    public byte[] getData() {
      return new byte[0];
    }

    /**
     * Prints the immutable value {@code InterledgerPreparePacket} with attribute values.
     *
     * @return A string representation of the value
     */
    @Override
    public String toString() {
      return "InterledgerPreparePacket{"
          + ", amount=" + getAmount()
          + ", expiresAt=" + getExpiresAt()
          + ", executionCondition=" + getExecutionCondition()
          + ", destination=" + getDestination()
          + ", data=" + Base64.getEncoder().encodeToString(getData())
          + ", typedData=" + typedData().orElse("n/a")
          + "}";
    }
  }

}
