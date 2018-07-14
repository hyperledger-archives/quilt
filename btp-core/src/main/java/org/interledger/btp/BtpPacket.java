package org.interledger.btp;

/*-
 * ========================LICENSE_START=================================
 * Bilateral Transfer Protocol Core Libs
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

import java.util.Optional;

public interface BtpPacket {

  BtpMessageType getType();

  long getRequestId();

  @Default
  default BtpSubProtocols getSubProtocols() {
    return BtpSubProtocols.empty();
  }

  /**
   * Get the primary {@link BtpSubProtocol}.
   *
   * @return the {@link BtpSubProtocol} that is first in the list
   */
  default BtpSubProtocol getPrimarySubProtocol() {
    if (getSubProtocols().isEmpty()) {
      throw new IndexOutOfBoundsException("No sub-protocols");
    }
    return getSubProtocols().get(0);
  }

  /**
   * Get the {@link BtpSubProtocol} by name.
   *
   * @param protocolName the name of the {@link BtpSubProtocol}
   *
   * @return a {@link BtpSubProtocol} or null if none exists with the given name
   */
  default Optional<BtpSubProtocol> getSubProtocol(String protocolName) {
    return getSubProtocols().stream()
        .filter(btpSubProtocol -> btpSubProtocol.getProtocolName().equals(protocolName))
        .findFirst();
  }

  /**
   * Check if a given {@link BtpSubProtocol} exists in this message.
   *
   * @param protocolName the name of the {@link BtpSubProtocol}
   *
   * @return a <code>true</code> if a {@link BtpSubProtocol} exists with the given name
   */
  default boolean hasSubProtocol(String protocolName) {
    return getSubProtocol(protocolName).isPresent();
  }

}
