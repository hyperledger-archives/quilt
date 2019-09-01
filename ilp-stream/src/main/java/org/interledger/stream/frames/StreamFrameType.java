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
import static org.interledger.stream.frames.StreamFrameConstants.CONNECTION_ASSET_DETAILS;
import static org.interledger.stream.frames.StreamFrameConstants.CONNECTION_CLOSE;
import static org.interledger.stream.frames.StreamFrameConstants.CONNECTION_DATA_BLOCKED;
import static org.interledger.stream.frames.StreamFrameConstants.CONNECTION_DATA_MAX;
import static org.interledger.stream.frames.StreamFrameConstants.CONNECTION_MAX_STREAM_ID;
import static org.interledger.stream.frames.StreamFrameConstants.CONNECTION_NEW_ADDRESS;
import static org.interledger.stream.frames.StreamFrameConstants.CONNECTION_STREAM_ID_BLOCKED;
import static org.interledger.stream.frames.StreamFrameConstants.STREAM_CLOSE;
import static org.interledger.stream.frames.StreamFrameConstants.STREAM_DATA;
import static org.interledger.stream.frames.StreamFrameConstants.STREAM_DATA_BLOCKED;
import static org.interledger.stream.frames.StreamFrameConstants.STREAM_DATA_MAX;
import static org.interledger.stream.frames.StreamFrameConstants.STREAM_MONEY;
import static org.interledger.stream.frames.StreamFrameConstants.STREAM_MONEY_BLOCKED;
import static org.interledger.stream.frames.StreamFrameConstants.STREAM_MONEY_MAX;

import java.util.Objects;

public enum StreamFrameType {

  ConnectionClose(CONNECTION_CLOSE),
  ConnectionNewAddress(CONNECTION_NEW_ADDRESS),
  ConnectionDataMax(CONNECTION_DATA_MAX),
  ConnectionDataBlocked(CONNECTION_DATA_BLOCKED),
  ConnectionMaxStreamId(CONNECTION_MAX_STREAM_ID),
  ConnectionStreamIdBlocked(CONNECTION_STREAM_ID_BLOCKED),
  ConnectionAssetDetails(CONNECTION_ASSET_DETAILS),

  StreamClose(STREAM_CLOSE),
  StreamMoney(STREAM_MONEY),
  StreamMoneyMax(STREAM_MONEY_MAX),
  StreamMoneyBlocked(STREAM_MONEY_BLOCKED),
  StreamData(STREAM_DATA),
  StreamDataMax(STREAM_DATA_MAX),
  StreamDataBlocked(STREAM_DATA_BLOCKED);

  private final short code;

  StreamFrameType(short code) {
    this.code = Objects.requireNonNull(code);
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
      case CONNECTION_CLOSE:
        return StreamFrameType.ConnectionClose;
      case CONNECTION_NEW_ADDRESS:
        return StreamFrameType.ConnectionNewAddress;
      case CONNECTION_DATA_MAX:
        return StreamFrameType.ConnectionDataMax;
      case CONNECTION_DATA_BLOCKED:
        return StreamFrameType.ConnectionDataBlocked;
      case CONNECTION_MAX_STREAM_ID:
        return StreamFrameType.ConnectionMaxStreamId;
      case CONNECTION_STREAM_ID_BLOCKED:
        return StreamFrameType.ConnectionStreamIdBlocked;
      case CONNECTION_ASSET_DETAILS:
        return StreamFrameType.ConnectionAssetDetails;
      case STREAM_CLOSE:
        return StreamFrameType.StreamClose;
      case STREAM_MONEY:
        return StreamFrameType.StreamMoney;
      case STREAM_MONEY_MAX:
        return StreamFrameType.StreamMoneyMax;
      case STREAM_MONEY_BLOCKED:
        return StreamFrameType.StreamMoneyBlocked;
      case STREAM_DATA:
        return StreamFrameType.StreamData;
      case STREAM_DATA_MAX:
        return StreamFrameType.StreamDataMax;
      case STREAM_DATA_BLOCKED:
        return StreamFrameType.StreamDataBlocked;
      default:
        throw new IllegalArgumentException(format("Unknown StreamFrame Type: %s", code));
    }

  }

  public short code() {
    return this.code;
  }
}

