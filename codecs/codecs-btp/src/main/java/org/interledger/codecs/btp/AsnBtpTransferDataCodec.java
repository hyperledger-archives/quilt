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

import org.interledger.btp.BtpTransfer;
import org.interledger.encoding.asn.codecs.AsnUint64Codec;

/**
 * Used to encode/decode the body of a MESSAGE and RESPONSE.
 */
public class AsnBtpTransferDataCodec extends AsnBtpPacketDataCodec<BtpTransfer> {


  /**
   * Default constructor.
   *
   * @param requestId the correlation id of the message/response
   */
  public AsnBtpTransferDataCodec(long requestId) {
    super(
        requestId,
        new AsnUint64Codec(), //Amount
        new AsnBtpSubProtocolsCodec() //SubProtocols
    );
  }

  /**
   * Decode and return the value read into the codec during serialization.
   *
   * @return the decoded object
   */
  @Override
  public BtpTransfer decode() {
    return BtpTransfer.builder()
        .requestId(getRequestId())
        .amount(getValueAt(0))
        .subProtocols(getValueAt(1))
        .build();
  }

  /**
   * Encode the provided value into the codec to be written during serialization.
   *
   * @param value the value to encode
   */
  @Override
  public void encode(BtpTransfer value) {
    setValueAt(0, value.getAmount());
    setValueAt(1, value.getSubProtocols());
  }

}
