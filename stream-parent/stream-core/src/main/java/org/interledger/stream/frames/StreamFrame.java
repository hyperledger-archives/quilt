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

/**
 * Contains STREAM messages of various type. Implementations MUST ignore frames with unknown types. Future versions of
 * STREAM may add new frame types.
 */
public interface StreamFrame {

  byte[] EMPTY_DATA = new byte[0];

  /**
   * The type of this frame.
   *
   * @return The {@link StreamFrameType} for this Stream Frame.
   */
  StreamFrameType streamFrameType();

}
