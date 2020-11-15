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

public interface InterledgerFulfillPacket extends InterledgerResponsePacket {

  /**
   * Get the default builder.
   *
   * @return a {@link InterledgerFulfillPacketBuilder} instance.
   */
  static InterledgerFulfillPacketBuilder builder() {
    return new InterledgerFulfillPacketBuilder();
  }

  /**
   * 32-byte preimage of the execution-condition from the corresponding ILP Prepare packet (found in {@link
   * InterledgerPreparePacket#getExecutionCondition()}).
   *
   * @return An instance of {@link InterledgerFulfillment}.
   */
  InterledgerFulfillment getFulfillment();

  @Immutable
  abstract class AbstractInterledgerFulfillPacket implements InterledgerFulfillPacket {

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
      return "InterledgerFulfillPacket{"
          + ", fulfillment=" + getFulfillment()
          + ", data=" + Base64.getEncoder().encodeToString(getData())
          + ", typedData=" + typedData().orElse("n/a")
          + "}";
    }
  }

}
