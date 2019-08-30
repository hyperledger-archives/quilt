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
 * <p>Indicates that the Stream was closed.</p>
 *
 * <p>If implementations allow half-open streams, an endpoint MAY continue sending money or data for this stream after
 * receiving a StreamClose frame. Otherwise, the endpoint MUST close the stream immediately.</p>
 */
public interface StreamClose extends StreamFrame {

  @Override
  default StreamFrameType streamFrameType() {
    return StreamFrameType.StreamClose;
  }

  /**
   * Identifier of the stream this frame refers to.
   *
   * @return
   */
  long streamId();

  /**
   * Machine-readable {@link ErrorCode} indicating why the Stream was closed.
   *
   * @return
   */
  ErrorCode errorCode();

  /**
   * Human-readable string intended to give more information helpful for debugging purposes.
   *
   * @return
   */
  String errorMessage();

  @Immutable
  abstract class AbstractStreamClose implements StreamClose {

    @Derived
    @Override
    public StreamFrameType streamFrameType() {
      return StreamFrameType.StreamClose;
    }

  }

}
