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

import org.interledger.annotations.Immutable;

import org.immutables.value.Value.Default;

import java.time.Instant;

public interface BtpError extends BtpPacket {

  static BtpErrorBuilder builder() {
    return new BtpErrorBuilder();
  }

  BtpErrorCode getErrorCode();

  default String getErrorName() {
    return getErrorCode().name();
  }

  default Instant getTriggeredAt() {
    return Instant.now();
  }

  default byte[] getErrorData() {
    return new byte[0];
  }

  @Immutable
  abstract class AbstractBtpError implements BtpError {

    @Override
    public final BtpMessageType getType() {
      return BtpMessageType.ERROR;
    }

    @Override
    @Default
    public String getErrorName() {
      return getErrorCode().name();
    }

    @Override
    @Default
    public Instant getTriggeredAt() {
      return Instant.now();
    }

    @Override
    @Default
    public byte[] getErrorData() {
      return new byte[0];
    }

    @Override
    @Default
    public BtpSubProtocols getSubProtocols() {
      return BtpSubProtocols.empty();
    }
  }

}
