package org.interledger.core;

/*-
 * ========================LICENSE_START=================================
 * Interledger Core
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

import java.util.Objects;

/**
 * A helper class for mapping an instance {@link InterledgerResponsePacket} according to the actual polymorphic type of
 * the passed-in instantiated object.
 */
public abstract class InterledgerResponsePacketHandler {

  /**
   * Handle the supplied {@code responsePacket} in a type-safe manner.
   *
   * @param responsePacket The generic {@link InterledgerResponsePacket} to be mapped in a type-safe manner.
   */
  public final void handle(final InterledgerResponsePacket responsePacket) {
    Objects.requireNonNull(responsePacket);

    if (InterledgerFulfillPacket.class.isAssignableFrom(responsePacket.getClass())) {
      handleFulfillPacket((InterledgerFulfillPacket) responsePacket);
    } else if (InterledgerRejectPacket.class.isAssignableFrom(responsePacket.getClass())) {
      handleRejectPacket((InterledgerRejectPacket) responsePacket);
    } else {
      throw new RuntimeException(
          String.format("Unsupported InterledgerResponsePacket Type: %s", responsePacket.getClass()));
    }
  }

  /**
   * Handle the packet as an {@link InterledgerPacket}.
   *
   * @param interledgerFulfillPacket The generic {@link InterledgerPacket} to be mapped in a type-safe manner.
   */
  protected abstract void handleFulfillPacket(final InterledgerFulfillPacket interledgerFulfillPacket);

  /**
   * Handle the packet as an {@link InterledgerPacket}.
   *
   * @param interledgerRejectPacket The generic {@link InterledgerPacket} to be mapped in a type-safe manner.
   */
  protected abstract void handleRejectPacket(final InterledgerRejectPacket interledgerRejectPacket);

}
