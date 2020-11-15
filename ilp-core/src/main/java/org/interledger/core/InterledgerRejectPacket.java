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
import java.util.Optional;

public interface InterledgerRejectPacket extends InterledgerResponsePacket {

  /**
   * Get the default builder.
   *
   * @return a {@link InterledgerRejectPacketBuilder} instance.
   */
  static InterledgerRejectPacketBuilder builder() {
    return new InterledgerRejectPacketBuilder();
  }

  /**
   * The Interledger Error Code for this error.
   *
   * @return An {@link InterledgerErrorCode}.
   */
  InterledgerErrorCode getCode();

  /**
   * The {@link InterledgerAddress} of the entity that originally emitted the error.
   *
   * @return An {@link InterledgerAddress}.
   */
  Optional<InterledgerAddress> getTriggeredBy();

  /**
   * User-readable error message, primarily intended for debugging purposes.
   *
   * @return a String.
   */
  String getMessage();

  @Immutable
  abstract class AbstractInterledgerRejectPacket implements InterledgerRejectPacket {

    @Override
    @Default
    public byte[] getData() {
      return new byte[0];
    }


    @Override
    @Default
    public String getMessage() {
      // In OER, an "empty" string is just a single 0 length-indicator byte to indicate no bytes. This maps well to an
      // empty string, so we don't use optional here
      return "";
    }

    /**
     * Prints the immutable value {@code InterledgerRejectPacket} with attribute values.
     *
     * @return A string representation of the value
     */
    @Override
    public String toString() {
      return "InterledgerRejectPacket{"
          + ", code=" + getCode()
          + ", triggeredBy=" + getTriggeredBy()
          + ", message=" + getMessage()
          + ", data=" + Base64.getEncoder().encodeToString(getData())
          + ", typedData=" + typedData().orElse("n/a")
          + "}";
    }
  }
}
