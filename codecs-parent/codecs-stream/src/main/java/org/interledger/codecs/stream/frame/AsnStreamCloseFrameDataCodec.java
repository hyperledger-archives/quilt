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
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;
import org.interledger.encoding.asn.codecs.AsnUint8Codec;
import org.interledger.encoding.asn.codecs.AsnUintCodecUL;
import org.interledger.encoding.asn.codecs.AsnUtf8StringCodec;
import org.interledger.stream.frames.ErrorCode;
import org.interledger.stream.frames.StreamCloseFrame;

public class AsnStreamCloseFrameDataCodec extends AsnSequenceCodec<StreamCloseFrame> {

  /**
   * Default constructor.
   */
  public AsnStreamCloseFrameDataCodec() {
    super(
        new AsnUintCodecUL(),
        new AsnUint8Codec(),
        new AsnUtf8StringCodec(AsnSizeConstraint.UNCONSTRAINED)
    );
  }

  @Override
  public StreamCloseFrame decode() {
    return StreamCloseFrame.builder()
        .streamId(getValueAt(0))
        .errorCode(ErrorCode.of(getValueAt(1)))
        .errorMessage(getValueAt(2))
        .build();
  }

  @Override
  public void encode(StreamCloseFrame value) {
    setValueAt(0, value.streamId());
    setValueAt(1, value.errorCode().getCode());
    setValueAt(2, value.errorMessage());
  }
}
