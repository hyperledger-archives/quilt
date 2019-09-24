package org.interledger.stream;

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

import org.interledger.core.Immutable;
import org.interledger.core.InterledgerPacketType;
import org.interledger.stream.frames.StreamFrame;
import org.interledger.stream.frames.StreamFrameType;

import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value.Derived;

import java.util.List;

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
@Immutable
public interface StreamPacket {

  // NOTE: Integer.MAX_VALUE is 1 less than what we want for our Max.
  UnsignedLong MAX_FRAMES_PER_CONNECTION = UnsignedLong.valueOf(Long.valueOf(Integer.MAX_VALUE) + 1L);

  short VERSION_1 = 1;

  /**
   * Get the default builder.
   *
   * @return a {@link StreamPacketBuilder} instance.
   */
  static StreamPacketBuilder builder() {
    return new StreamPacketBuilder();
  }

  /**
   * The version of this packet, currently 1.
   *
   * @return This packet's STREAM version (always 1).
   */
  default short version() {
    return VERSION_1;
  }

  /**
   * ILPv4 packet type this STREAM packet MUST be sent in (12 for Prepare, 13 for Fulfill, and 14 for Reject).
   *
   * @return A {@link InterledgerPacketType}
   */
  InterledgerPacketType interledgerPacketType();

  /**
   * Identifier for an ILP request / response. Clients and Servers track their own outgoing packet sequence numbers and
   * increment the Sequence for each ILP Prepare they send. The Receiver MUST respond with a STREAM packet that includes
   * the same Sequence as the Sender's Prepare packet. A sender MUST discard a STREAM packet in which the Sequence does
   * not match the STREAM packet sent with their ILP Prepare.
   *
   * @return A {@link UnsignedLong} for this packet's sequence.
   */
  UnsignedLong sequence();

  /**
   * Determines if this packet contains a {@link #sequence()} that can be safely used to encrypt data using a single
   * shared secret. Per IL-RFC-29, "Implementations MUST close the connection once either endpoint has sent 2^31
   * packets. According to NIST, it is unsafe to use AES-GCM for more than 2^32 packets using the same encryption key
   * (STREAM uses the limit of 2^31 because both endpoints encrypt packets with the same key).
   *
   * @return {@code true} if the current sequence can safely be used with a single shared-secret; {@code false}
   *     otherwise.
   */
  @Derived
  default boolean sequenceIsSafeForSingleSharedSecret() {
    // Only return true if the `sequence` is below MAX_FRAMES_PER_CONNECTION. At or above is unsafe.
    return this.sequence().compareTo(MAX_FRAMES_PER_CONNECTION) < 0;
  }

  /**
   * If the STREAM packet is sent on an ILP Prepare, this represents the minimum the receiver should accept (this is a
   * signal between the sender and receiver that cannot be tampered with by intermediate nodes, and acts as a safety net
   * against fees that might be too high in the newtwork). If the packet is sent on an ILP Fulfill or Reject, this
   * represents the amount that the receiver got in the Prepare.
   *
   * @return A {@link UnsignedLong} for this packet's prepareAmount.
   */
  UnsignedLong prepareAmount();

  /**
   * A list of {@link StreamFrameType}.
   *
   * @return A {@link List} of Stream Frames that this packet contains.
   */
  List<StreamFrame> frames();

}
