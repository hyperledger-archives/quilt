package org.interledger.ildcp;

/*-
 * ========================LICENSE_START=================================
 * Interledger DCP Core
 * %%
 * Copyright (C) 2017 - 2019 Hyperledger and its contributors
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
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;

import org.immutables.value.Value.Default;

/**
 * An extension of {@link InterledgerFulfillPacket} that is also a {@link IldcpResponsePacket} that can be used as an
 * IL-DCP response over Interledger.
 */
public interface IldcpResponsePacket extends InterledgerFulfillPacket {

  InterledgerFulfillment EXECUTION_FULFILLMENT = InterledgerFulfillment.of(new byte[32]);

  static IldcpResponsePacketBuilder builder() {
    return new IldcpResponsePacketBuilder();
  }

  /**
   * The fulfillment of an ILP packet for IL-DCP is always a 32-byte octet string all filled with zeros.
   */
  default InterledgerFulfillment getFulfillment() {
    return EXECUTION_FULFILLMENT;
  }

  /**
   * The {@link IldcpResponse} encoded into the <tt>data</tt> field of this packet.
   *
   * @return The {@link IldcpResponse}.
   */
  IldcpResponse getIldcpResponse();

  /**
   * Exists to satisfy Immutables.
   */
  @Immutable
  abstract class AbstractIldcpResponsePacket implements IldcpResponsePacket {

    /**
     * The fulfillment of an ILP packet for IL-DCP is always a 32-byte octet string all filled with zeros.
     */
    @Default
    @Override
    public InterledgerFulfillment getFulfillment() {
      return EXECUTION_FULFILLMENT;
    }

    // Overridden because in the general case, `data` is only used when serialization occurs.
    @Override
    @Default
    public byte[] getData() {
      return new byte[0];
    }

  }
}
