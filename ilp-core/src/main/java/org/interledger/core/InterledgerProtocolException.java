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

import java.util.Objects;

/**
 * Base ILP exception, see RFC REF: https://interledger.org/rfcs/0003-interledger-protocol/#errors
 */
public class InterledgerProtocolException extends InterledgerRuntimeException {

  private static final long serialVersionUID = 1L;

  private final InterledgerRejectPacket interledgerRejectPacket;

  /**
   * Required-args constructor.
   *
   * @param interledgerRejectPacket An instance of {@link InterledgerRejectPacket} that is the
   *                                 underlying error encapsulated by this exception.
   */
  public InterledgerProtocolException(final InterledgerRejectPacket interledgerRejectPacket) {
    super("Interledger Rejection.");
    this.interledgerRejectPacket = fillInterledgerRejectPacket(interledgerRejectPacket);
  }

  /**
   * Constructs a new Interledger protocol exception with the specified reject packet and
   * detail message.
   *
   * @param interledgerRejectPacket An instance of {@link InterledgerRejectPacket} that is the
   *                                 underlying error encapsulated by this exception.
   * @param message The detail message.
   */
  public InterledgerProtocolException(final InterledgerRejectPacket interledgerRejectPacket,
      final String message) {
    super(message);
    this.interledgerRejectPacket = fillInterledgerRejectPacket(interledgerRejectPacket);
  }

  public InterledgerRejectPacket getInterledgerRejectPacket() {
    return interledgerRejectPacket;
  }

  private InterledgerRejectPacket fillInterledgerRejectPacket(
      final InterledgerRejectPacket interledgerRejectPacket
  ) {
    return Objects
        .requireNonNull(interledgerRejectPacket, "interledgerRejectPacket must not be null");
  }
}
