package org.interledger.core;

/*-
 * ========================LICENSE_START=================================
 * Interledger Core
 * %%
 * Copyright (C) 2017 - 2018 Interledger
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

import java.util.Arrays;

public interface InterledgerRejectPacket extends InterledgerPacket {

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
  InterledgerAddress getTriggeredBy();

  /**
   * The {@link InterledgerAddress} of the entity that originally emitted the error.
   *
   * @return An {@link InterledgerAddress}.
   */
  String getMessage();

  /**
   * Machine-readable data. The format is defined for each error code. Implementations MUST follow
   *     the correct format for the code given in the `code` field.
   *
   * @return The optional error data.
   */
  byte[] getData();

  @Immutable
  abstract class AbstractInterledgerRejectPacket implements InterledgerRejectPacket {

    @Override
    @Default
    public byte[] getData() {
      return new byte[0];
    }
  }
}
