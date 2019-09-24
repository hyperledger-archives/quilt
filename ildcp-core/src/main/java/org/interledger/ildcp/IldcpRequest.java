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
import org.interledger.core.InterledgerCondition;

import org.immutables.value.Value.Default;

import java.math.BigInteger;
import java.time.Instant;

/**
 * A request to a Connector to get child account information.
 */
public interface IldcpRequest {

  InterledgerCondition EXECUTION_CONDITION = IldcpResponsePacket.EXECUTION_FULFILLMENT.getCondition();

  /**
   * Get the default builder.
   *
   * @return a {@link IldcpRequestBuilder} instance.
   */
  static IldcpRequestBuilder builder() {
    return new IldcpRequestBuilder();
  }

  /**
   * The destination of an ILP packet for IL-DCP is <tt>0</tt> by default, but can be adjusted.
   */
  default BigInteger getAmount() {
    return BigInteger.ZERO;
  }

  /**
   * The Date and time when the packet expires. Each connector changes the value of this field to set the expiry to an
   * earlier time, before forwarding the packet.
   *
   * @return The {@link Instant} this packet should be considered to be expired.
   */
  Instant getExpiresAt();

  /**
   * Exists to satisfy Immutables.
   */
  @Immutable
  abstract class AbstractIldcpRequest implements IldcpRequest {

    @Override
    @Default
    public BigInteger getAmount() {
      return BigInteger.ZERO;
    }

    @Override
    @Default
    public Instant getExpiresAt() {
      return Instant.now().plusSeconds(30);
    }
  }

}
