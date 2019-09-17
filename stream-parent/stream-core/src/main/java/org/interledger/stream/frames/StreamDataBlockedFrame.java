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
import org.interledger.core.Immutable;

/**
 * A STREAM frame used to indicate that data has been blocked.
 */
@Immutable
public interface StreamDataBlockedFrame extends StreamFrame {

  /**
   * Get the default builder.
   *
   * @return a {@link StreamDataBlockedFrameBuilder} instance.
   */
  static StreamDataBlockedFrameBuilder builder() {
    return new StreamDataBlockedFrameBuilder();
  }

  @Override
  default StreamFrameType streamFrameType() {
    return StreamFrameType.StreamDataBlocked;
  }

  /**
   * Identifier of the stream this frame refers to.
   *
   * @return An {@link UnsignedLong} containing the stream id.
   */
  UnsignedLong streamId();

  /**
   * The total number of bytes the endpoint wants to send on this stream.
   *
   * @return An {@link UnsignedLong} containing the max offset.
   */
  UnsignedLong maxOffset();

}
