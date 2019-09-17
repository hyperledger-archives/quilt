package org.interledger.stream.frames;

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

import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value;
import org.interledger.core.Immutable;

/**
 * <p>Packets may be received out of order so the Offset is used to indicate the correct position of the byte segment
 * in the overall stream. The first StreamData frame sent for a given stream MUST start with an Offset of zero.</p>
 */
@Immutable
public interface StreamDataFrame extends StreamFrame {

  /**
   * Get the default builder.
   *
   * @return a {@link StreamDataFrameBuilder} instance.
   */
  static StreamDataFrameBuilder builder() {
    return new StreamDataFrameBuilder();
  }

  @Override
  default StreamFrameType streamFrameType() {
    return StreamFrameType.StreamData;
  }

  /**
   * Identifier of the stream this frame refers to.
   *
   * @return An {@link UnsignedLong} containing the stream id for this frame.
   */
  UnsignedLong streamId();

  /**
   * Position of this data in the byte stream.
   *
   * @return An {@link UnsignedLong} containing the offset for this frame.
   */
  UnsignedLong offset();

  /**
   * Application data.
   *
   * @return A byte array with STREAM data.
   */
  @Value.Default
  default byte[] data() {
    return EMPTY_DATA;
  }

}
