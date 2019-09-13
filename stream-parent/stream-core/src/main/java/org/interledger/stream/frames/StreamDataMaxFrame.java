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

import org.interledger.core.Immutable;

import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value.Derived;

/**
 * <p>The amounts in this frame are denominated in the units of the endpoint sending the frame, so the other endpoint
 * must use their calculated exchange rate to determine how much more they can send for this stream.</p>
 */
public interface StreamDataMaxFrame extends StreamFrame {

  /**
   * Get the default builder.
   *
   * @return a {@link StreamDataMaxFrameBuilder} instance.
   */
  static StreamDataMaxFrameBuilder builder() {
    return new StreamDataMaxFrameBuilder();
  }

  @Override
  default StreamFrameType streamFrameType() {
    return StreamFrameType.StreamDataMax;
  }

  /**
   * Identifier of the stream this frame refers to.
   *
   * @return An {@link UnsignedLong} containing the stream id for this frame.
   */
  UnsignedLong streamId();

  /**
   * The total number of bytes the endpoint is willing to receive on this stream.
   *
   * @return An {@link UnsignedLong} containing the max offset for this frame.
   */
  UnsignedLong maxOffset();

  @Immutable
  abstract class AbstractStreamDataMaxFrame implements StreamDataMaxFrame {

    @Derived
    @Override
    public StreamFrameType streamFrameType() {
      return StreamFrameType.StreamDataMax;
    }

  }

}
