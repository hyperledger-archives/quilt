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
import org.interledger.encoding.asn.codecs.AsnUint64Codec;
import org.interledger.encoding.asn.codecs.AsnUint64CodecUL;
import org.interledger.encoding.asn.codecs.AsnUintCodecUL;
import org.interledger.stream.frames.StreamMoneyBlockedFrame;

public class AsnStreamMoneyBlockedFrameDataCodec extends AsnSequenceCodec<StreamMoneyBlockedFrame> {

  /**
   * Default constructor.
   */
  public AsnStreamMoneyBlockedFrameDataCodec() {
    super(
        new AsnUintCodecUL(),
        new AsnUintCodecUL(),
        new AsnUintCodecUL()
    );
  }

  @Override
  public StreamMoneyBlockedFrame decode() {
    return StreamMoneyBlockedFrame.builder()
        .streamId(getValueAt(0))
        .sendMax(getValueAt(1))
        .totalSent(getValueAt(2))
        .build();
  }

  @Override
  public void encode(StreamMoneyBlockedFrame value) {
    setValueAt(0, value.streamId());
    setValueAt(1, value.sendMax());
    setValueAt(2, value.totalSent());
  }
}
