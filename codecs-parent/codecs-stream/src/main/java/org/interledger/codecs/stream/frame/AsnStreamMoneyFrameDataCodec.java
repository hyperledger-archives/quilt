package org.interledger.codecs.stream.frame;

/*-
 * ========================LICENSE_START=================================
 * Interledger Core Codecs
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

import org.interledger.encoding.asn.codecs.AsnSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnUintCodecUL;
import org.interledger.stream.frames.StreamMoneyFrame;

public class AsnStreamMoneyFrameDataCodec extends AsnSequenceCodec<StreamMoneyFrame> {

  /**
   * Default constructor.
   */
  public AsnStreamMoneyFrameDataCodec() {
    super(
        new AsnUintCodecUL(),
        new AsnUintCodecUL()
    );
  }

  @Override
  public StreamMoneyFrame decode() {
    return StreamMoneyFrame.builder()
        .streamId(getValueAt(0))
        .shares(getValueAt(1))
        .build();
  }

  @Override
  public void encode(StreamMoneyFrame value) {
    setValueAt(0, value.streamId());
    setValueAt(1, value.shares());
  }
}
