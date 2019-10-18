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
import org.interledger.core.DateUtils;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Derived;

import java.time.Instant;
import java.util.Base64;

public interface BtpError extends BtpResponsePacket {

  static BtpErrorBuilder builder() {
    return new BtpErrorBuilder();
  }

  /**
   * A standardized {@link BtpErrorCode} for this error.
   */
  BtpErrorCode getErrorCode();

  /**
   * The time of emission.
   */
  default Instant getTriggeredAt() {
    return DateUtils.now();
  }

  /**
   * Additional data for this BTP Error.
   */
  default byte[] getErrorData() {
    return new byte[0];
  }

  @Immutable
  abstract class AbstractBtpError implements BtpError {

    @Override
    @Derived
    public BtpMessageType getType() {
      return BtpMessageType.ERROR;
    }

    @Override
    @Default
    public Instant getTriggeredAt() {
      return DateUtils.now();
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

    /**
     * Prints the immutable value {@code BtpError} with attribute values.
     *
     * @return A string representation of the value
     */
    @Override
    public String toString() {
      return "BtpError{"
          + " requestId=" + getRequestId()
          + ", triggeredAt=" + getTriggeredAt()
          + ", errorData=" + Base64.getEncoder().encodeToString(getErrorData())
          + ", subProtocols=" + getSubProtocols()
          + ", errorCode=" + getErrorCode()
          + "}";
    }
  }

}
