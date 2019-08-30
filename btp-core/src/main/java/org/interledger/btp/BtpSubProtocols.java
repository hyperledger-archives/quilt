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

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

public class BtpSubProtocols extends ArrayList<BtpSubProtocol> {

  //TODO Optimize implementation by indexing by protocol name too

  public static final String AUTH = "auth";
  public static final String AUTH_TOKEN = "auth_token";
  public static final String AUTH_USERNAME = "auth_username";
  public static final String INTERLEDGER = "ilp";

  /**
   * Create a new {@link BtpSubProtocols} list with no sub-protocols.
   *
   * @return a new {@link BtpSubProtocols} list with no sub-protocols.
   */
  public static BtpSubProtocols empty() {
    return new BtpSubProtocols();
  }

  /**
   * Create a new {@link BtpSubProtocols} list with the given {@link BtpSubProtocol} as the primary sub-protocol.
   *
   * @param protocol the sub-protocol to use as the primary
   *
   * @return a new {@link BtpSubProtocols} list with only a primary sub-protocol
   */
  public static BtpSubProtocols fromPrimarySubProtocol(BtpSubProtocol protocol) {
    BtpSubProtocols subProtocols = new BtpSubProtocols();
    subProtocols.add(protocol);
    return subProtocols;
  }

  /**
   * Get the primary {@link BtpSubProtocol}.
   *
   * @return the {@link BtpSubProtocol} that is first in the list
   */
  public BtpSubProtocol getPrimarySubProtocol() {
    return get(0);
  }

  /**
   * Check if a given {@link BtpSubProtocol} exists in this list.
   *
   * @param protocolName the name of the {@link BtpSubProtocol}
   *
   * @return a <code>true</code> if a {@link BtpSubProtocol} exists with the given name
   */
  public boolean hasSubProtocol(String protocolName) {
    for (BtpSubProtocol protocol : this) {
      if (protocol.getProtocolName().equals(protocolName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Accessor for the {@link BtpSubProtocol} with name of {@code subprotocolName}.
   *
   * @param subprotocolName the name of the {@link BtpSubProtocol} to retrieve.
   *
   * @return a {@link BtpSubProtocol} or null if none exists with the given name
   */
  public Optional<BtpSubProtocol> getSubProtocol(final String subprotocolName) {
    Objects.requireNonNull(subprotocolName);
    return this.stream()
        .filter(btpSubProtocol -> btpSubProtocol.getProtocolName().equals(subprotocolName))
        .findFirst();
  }
}
