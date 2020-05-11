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

import java.util.Optional;

public interface InterledgerPacket {

  /**
   * End-to-end data. Connectors MUST NOT modify this data. Most higher-level protocols will encrypt and authenticate
   * this data, so receivers will reject packets in which the data is modified
   *
   * @return A byte array.
   */
  byte[] getData();

  /**
   * Typed variant of {@link #getData}. This method exists so that implementations can constructed object (e.g., a
   * StreamPacket) and use them in various places in code without having to decode this object's bytes more than once.
   *
   * @return An optionally-present {@link Object} that can be cast to an appropriate type.
   */
  Optional<Object> typedData();
}
