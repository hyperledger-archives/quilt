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
  RESPONSE(1),
  ERROR(2),
  MESSAGE(6),
  TRANSFER(7);


  private final int code;

  BtpMessageType(int code) {
    this.code = code;
  }

  public int getCode() {
    return this.code;
  }

  /**
   * Get a new {@link BtpMessageType} from the code.
   *
   * @param code the message type code.
   * @return A new {@link BtpMessageType} from the provided code
   */
  public static BtpMessageType fromCode(int code) {

    switch (code) {
      case 1:
        return BtpMessageType.RESPONSE;
      case 2:
        return BtpMessageType.ERROR;
      case 6:
        return BtpMessageType.MESSAGE;
      case 7:
        return BtpMessageType.TRANSFER;
      default:
        throw new IllegalArgumentException(format("Unknown BTP Message Type: %s", code));
    }

  }
}

