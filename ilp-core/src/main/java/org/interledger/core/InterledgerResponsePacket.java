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

import org.immutables.value.Value.Default;

import java.util.Base64;

/**
 * An extension of {@link InterledgerPacket} that indicates a response in an Interledger flow. Per RFC-4, an Interledger
 * prepare packet is sent to a remote counterparty, and the response is either a Reject or a Fulfill. This interface
 * allows both of those response packets to to be handled as one type.
 */
public interface InterledgerResponsePacket extends InterledgerPacket {

  /**
   * Get the default builder.
   *
   * @return a {@link InterledgerFulfillPacketBuilder} instance.
   */
  static InterledgerResponsePacketBuilder builder() {
    return new InterledgerResponsePacketBuilder();
  }

  @Immutable
  abstract class AbstractInterledgerResponsePacket implements InterledgerResponsePacket {

    @Override
    @Default
    public byte[] getData() {
      return new byte[0];
    }

    /**
     * Prints the immutable value {@code InterledgerFulfillPacket} with attribute values.
     *
     * @return A string representation of the value
     */
    @Override
    public String toString() {
      return "InterledgerResponsePacket{"
          + ", data=" + Base64.getEncoder().encodeToString(getData())
          + "}";
    }
  }

}
