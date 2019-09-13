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
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Derived;

/**
 * <p>The amounts in this frame are denominated in the units of the endpoint sending the frame, so the other endpoint
 * must use their calculated exchange rate to determine how much more they can send for this stream.</p>
 */
public interface StreamMoneyMaxFrame extends StreamFrame {

  /**
   * Get the default builder.
   *
   * @return a {@link StreamMoneyMaxFrameBuilder} instance.
   */
  static StreamMoneyMaxFrameBuilder builder() {
    return new StreamMoneyMaxFrameBuilder();
  }

  @Override
  default StreamFrameType streamFrameType() {
    return StreamFrameType.StreamMoneyMax;
  }

  /**
   * Identifier of the stream this frame refers to.
   *
   * @return
   */
  UnsignedLong streamId();

  /**
   * Total amount, denominated in the units of the endpoint sending this frame, that the endpoint is willing to receive
   * on this stream.
   *
   * @return
   */
  UnsignedLong receiveMax();

  /**
   * Total amount, denominated in the units of the endpoint sending this frame, that the endpoint has received thus
   * far.
   *
   * @return
   */
  default UnsignedLong totalReceived() {
    return UnsignedLong.ZERO;
  }

  @Immutable
  abstract class AbstractStreamMoneyMaxFrame implements StreamMoneyMaxFrame {

    @Derived
    @Override
    public StreamFrameType streamFrameType() {
      return StreamFrameType.StreamMoneyMax;
    }

    @Default
    @Override
    public UnsignedLong totalReceived() {
      return UnsignedLong.ZERO;
    }
  }

}