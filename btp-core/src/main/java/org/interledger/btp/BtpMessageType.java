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

import static java.lang.String.format;

public enum BtpMessageType {

  RESPONSE((short) 1),
  ERROR((short) 2),
  MESSAGE((short) 6),
  TRANSFER((short) 7);

  private final short code;

  BtpMessageType(short code) {
    this.code = code;
  }

  /**
   * Get a new {@link BtpMessageType} from the code.
   *
   * @param code the message type code.
   *
   * @return A new {@link BtpMessageType} from the provided code
   */
  public static BtpMessageType fromCode(short code) {

    switch (code) {
      case (short) 1:
        return BtpMessageType.RESPONSE;
      case (short) 2:
        return BtpMessageType.ERROR;
      case (short) 6:
        return BtpMessageType.MESSAGE;
      case (short) 7:
        return BtpMessageType.TRANSFER;
      default:
        throw new IllegalArgumentException(format("Unknown BTP Message Type: %s", code));
    }

  }

  public short getCode() {
    return this.code;
  }
}

