package org.interledger.btp;

/*-
 * ========================LICENSE_START=================================
 * Bilateral Transfer Protocol Core Libs
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

import org.interledger.btp.ImmutableBtpSessionCredentials.Builder;

import org.immutables.value.Value;
import org.immutables.value.Value.Redacted;

import java.util.Optional;

/**
 * Authentication credentials for a {@link BtpSession}.
 */
@Value.Immutable
public interface BtpSessionCredentials {

  static Builder builder() {
    return ImmutableBtpSessionCredentials.builder();
  }

  /**
   * <p>The `auth_username` for a BTP client. Enables multiple accounts over a single BTP WebSocket connection.</p>
   *
   * @return
   */
  Optional<String> getAuthUsername();

  /**
   * The <tt>auth_token</tt> for a BTP client, as specified in IL-RFC-23.
   *
   * @return
   *
   * @see "https://github.com/interledger/rfcs/blob/master/0023-bilateral-transfer-protocol
   *     /0023-bilateral-transfer-protocol.md#authentication"
   */
  @Redacted
  String getAuthToken();

}
