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

import org.interledger.annotations.Immutable;

import org.immutables.value.Value.Default;

import java.math.BigInteger;
import java.time.Instant;

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

  InterledgerCondition getExecutionCondition();

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
    @Default
    public byte[] getData() {
      return new byte[0];
    }
  }

}
