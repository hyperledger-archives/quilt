package org.interledger.stream.frames;

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

import java.util.Objects;

public enum StreamFrameType {

  ConnectionClose((short) 1),
  ConnectionNewAddress((short) 2),
  ConnectionMaxData((short) 3),
  ConnectionDataBlocked((short) 4),
  ConnectionMaxStreamId((short) 5),
  ConnectionStreamIdBlocked((short) 6),
  ConnectionAssetDetails((short) 7),

  StreamClose((short) 10),
  StreamMoney((short) 11),
  StreamMaxMoney((short) 12),
  StreamMoneyBlocked((short) 13),
  StreamData((short) 14),
  StreamMaxData((short) 15),
  StreamDataBlocked((short) 16);

  private final short type;

  StreamFrameType(short type) {
    this.type = Objects.requireNonNull(type);
  }

  /**
   * Get a new {@link StreamFrameType} from the type.
   *
   * @param code the message type type.
   *
   * @return A new {@link StreamFrameType} from the provided type
   */
  public static StreamFrameType fromCode(short code) {

    switch (code) {
      case (short) 1:
        return StreamFrameType.ConnectionClose;
      case (short) 2:
        return StreamFrameType.ConnectionNewAddress;
      case (short) 3:
        return StreamFrameType.ConnectionMaxData;
      case (short) 4:
        return StreamFrameType.ConnectionDataBlocked;
      case (short) 5:
        return StreamFrameType.ConnectionMaxStreamId;
      case (short) 6:
        return StreamFrameType.ConnectionStreamIdBlocked;
      case (short) 7:
        return StreamFrameType.ConnectionAssetDetails;
      case (short) 10:
        return StreamFrameType.StreamClose;
      case (short) 11:
        return StreamFrameType.StreamMoney;
      case (short) 12:
        return StreamFrameType.StreamMaxMoney;
      case (short) 13:
        return StreamFrameType.StreamMoneyBlocked;
      case (short) 14:
        return StreamFrameType.StreamData;
      case (short) 15:
        return StreamFrameType.StreamMaxData;
      case (short) 16:
        return StreamFrameType.StreamDataBlocked;
      default:
        throw new IllegalArgumentException(format("Unknown StreamFrame Type: %s", code));
    }

  }

  public short getType() {
    return this.type;
  }
}

