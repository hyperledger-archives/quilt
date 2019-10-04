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

/**
 * Indicates that the connection was closed.
 */
@Immutable
public interface ConnectionMaxStreamIdFrame extends StreamFrame {

  /**
   * Get the default builder.
   *
   * @return a {@link ConnectionMaxStreamIdFrameBuilder} instance.
   */
  static ConnectionMaxStreamIdFrameBuilder builder() {
    return new ConnectionMaxStreamIdFrameBuilder();
  }

  @Override
  default StreamFrameType streamFrameType() {
    return StreamFrameType.ConnectionMaxStreamId;
  }

  /**
   * The maximum stream ID the endpoint is willing to accept.
   *
   * @return An {@link UnsignedLong} containing the max stream id.
   */
  UnsignedLong maxStreamId();

}
