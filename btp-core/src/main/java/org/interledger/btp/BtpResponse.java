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

import org.interledger.core.Immutable;

import org.immutables.value.Value.Derived;

/**
 * Returned from a peer if the peer acknowledges a {@link BtpMessage} or {@link BtpTransfer}. If the peer has data to
 * send in reply (e.g. a quote response), it is carried in the protocol data of this response. In addition, if a
 * Response has been returned for a {@link BtpTransfer}, balances MUST have been updated.
 */
public interface BtpResponse extends BtpResponsePacket {

  static BtpResponseBuilder builder() {
    return new BtpResponseBuilder();
  }

  @Immutable
  abstract class AbstractBtpResponse implements BtpResponse {

    @Override
    @Derived
    public BtpMessageType getType() {
      return BtpMessageType.RESPONSE;
    }

  }

}
