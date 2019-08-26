package org.interledger.stream;

/*-
 * ========================LICENSE_START=================================
 * Bilateral Transfer Protocol Core Libs
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

import static java.lang.String.format;

public enum StreamFrame {

  ConnectionError((short) 1),
  ConnectionNewAddress((short) 2),
  ConnectionMaxData((short) 3),
  ConnectionDataBlocked((short) 4),
  ConnectionMaxStreamId((short) 5),
  ConnectionStreamIdBlocked((short) 6),


  StreamClose((short) 16),
  StreamMoney((short) 17),
  StreamMaxMoney((short) 18),
  StreamMoneyBlocked((short) 19),
  StreamData((short) 20),
  StreamMaxData((short) 21),
  StreamDataBlocked((short) 22);

  private final short type;

  StreamFrame(short type) {
    this.type = type;
  }

  /**
   * Get a new {@link StreamFrame} from the type.
   *
   * @param code the message type type.
   *
   * @return A new {@link StreamFrame} from the provided type
   */
  public static StreamFrame fromCode(short code) {

    switch (code) {
      case (short) 1:
        return StreamFrame.ConnectionError;
      case (short) 2:
        return StreamFrame.ConnectionNewAddress;
      case (short) 3:
        return StreamFrame.ConnectionMaxData;
      case (short) 4:
        return StreamFrame.ConnectionDataBlocked;
      case (short) 5:
        return StreamFrame.ConnectionNewAddress;
      case (short) 6:
        return StreamFrame.ConnectionNewAddress;
      case (short) 16:
        return StreamFrame.ConnectionNewAddress;
      case (short) 17:
        return StreamFrame.ConnectionNewAddress;
      case (short) 18:
        return StreamFrame.ConnectionNewAddress;
      case (short) 19:
        return StreamFrame.ConnectionNewAddress;
      case (short) 20:
        return StreamFrame.ConnectionNewAddress;
      case (short) 21:
        return StreamFrame.ConnectionNewAddress;
      case (short) 22:
        return StreamFrame.ConnectionNewAddress;
      default:
        throw new IllegalArgumentException(format("Unknown StreamFrame Type: %s", code));
    }

  }

  public short getType() {
    return this.type;
  }
}

