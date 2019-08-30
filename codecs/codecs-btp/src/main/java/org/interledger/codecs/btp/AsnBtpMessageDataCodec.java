package org.interledger.codecs.btp;

/*-
 * ========================LICENSE_START=================================
 * Bilateral Transfer Protocol Core Codecs
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

import org.interledger.btp.BtpMessage;

public class AsnBtpMessageDataCodec extends AsnBtpPacketDataCodec<BtpMessage> {


  /**
   * Default constructor.
   *
   * @param requestId the correlation id of the message/response
   */
  public AsnBtpMessageDataCodec(long requestId) {
    super(
        requestId,
        new AsnBtpSubProtocolsCodec() //SubProtocols
    );
  }

  /**
   * Decode and return the value read into the codec during serialization.
   *
   * @return the decoded object
   */
  @Override
  public BtpMessage decode() {
    return BtpMessage.builder()
        .requestId(getRequestId())
        .subProtocols(getValueAt(0))
        .build();
  }

  /**
   * Encode the provided value into the codec to be written during serialization.
   *
   * @param value the value to encode
   */
  @Override
  public void encode(BtpMessage value) {
    setValueAt(0, value.getSubProtocols());
  }

}
