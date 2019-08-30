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

import org.immutables.value.Value.Derived;

/**
 * A STREAM frame used to indicate that data has been blocked.
 */
public interface StreamDataBlockedFrame extends StreamFrame {

  @Override
  default StreamFrameType streamFrameType() {
    return StreamFrameType.StreamMoneyBlocked;
  }

  /**
   * Identifier of the stream this frame refers to.
   *
   * @return
   */
  long streamId();

  /**
   * The total number of bytes the endpoint wants to send on this stream.
   *
   * @return
   */
  long maxOffset();

  @Immutable
  abstract class AbstractStreamDataBlockedFrame implements StreamDataBlockedFrame {

    @Derived
    @Override
    public StreamFrameType streamFrameType() {
      return StreamFrameType.StreamMoneyBlocked;
    }

  }

}
