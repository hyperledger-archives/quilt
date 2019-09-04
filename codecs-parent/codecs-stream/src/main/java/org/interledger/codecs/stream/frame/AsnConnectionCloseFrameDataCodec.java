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

import org.interledger.encoding.asn.codecs.AsnIA5StringCodec;
import org.interledger.encoding.asn.codecs.AsnSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;
import org.interledger.encoding.asn.codecs.AsnUint8Codec;
import org.interledger.stream.frames.ConnectionCloseFrame;
import org.interledger.stream.frames.ErrorCode;

import java.util.Optional;

public class AsnConnectionCloseFrameDataCodec extends AsnSequenceCodec<ConnectionCloseFrame> {

  /**
   * Default constructor.
   */
  public AsnConnectionCloseFrameDataCodec() {
    super(
        new AsnUint8Codec(),// Error Code
        new AsnIA5StringCodec(AsnSizeConstraint.UNCONSTRAINED)// Error Message
    );
  }

  @Override
  public ConnectionCloseFrame decode() {
    return ConnectionCloseFrame.builder()
        .errorCode(ErrorCode.of(getValueAt(0)))
        .errorMessage(Optional.<String>ofNullable(getValueAt(1)).filter($ -> !"".equals($)))
        .build();
  }

  @Override
  public void encode(ConnectionCloseFrame value) {
    setValueAt(0, value.errorCode().getCode());
    setValueAt(1, value.errorMessage().orElse(""));
  }
}
