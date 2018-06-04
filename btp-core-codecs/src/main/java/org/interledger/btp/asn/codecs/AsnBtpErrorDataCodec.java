package org.interledger.btp.asn.codecs;

/*-
 * ========================LICENSE_START=================================
 * Bilateral Transfer Protocol Core Codecs
 * %%
 * Copyright (C) 2017 - 2018 Interledger
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

import org.interledger.btp.BtpError;
import org.interledger.btp.BtpErrorCode;
import org.interledger.encoding.asn.codecs.AsnIA5StringCodec;
import org.interledger.encoding.asn.codecs.AsnOctetStringCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;
import org.interledger.encoding.asn.codecs.AsnUtf8StringCodec;

/**
 * Used to encode/decode the body of a MESSAGE and RESPONSE.
 */
public class AsnBtpErrorDataCodec extends AsnBtpPacketDataCodec<BtpError> {


  /**
   * Default constructor.
   *
   * @param requestId the correlation id of the message/response
   */
  public AsnBtpErrorDataCodec(long requestId) {
    super(
        requestId,
        new AsnUtf8StringCodec(new AsnSizeConstraint(3)), //Code
        new AsnUtf8StringCodec(AsnSizeConstraint.UNCONSTRAINED), //Name
        new AsnIA5StringCodec(AsnSizeConstraint.UNCONSTRAINED), //TODO Parse time
        new AsnOctetStringCodec(AsnSizeConstraint.UNCONSTRAINED), //Data
        new AsnBtpSubProtocolsCodec() //SubProtocols
    );
  }

  /**
   * Decode and return the value read into the codec during serialization.
   *
   * @return the decoded object
   */
  @Override
  public BtpError decode() {
    return BtpError.builder()
        .requestId(getRequestId())
        .errorCode(BtpErrorCode.fromString(getValueAt(0)))
        .errorName(getValueAt(1))
        .triggeredAt(getValueAt(2))
        .errorData(getValueAt(3))
        .subProtocols(getValueAt(4))
        .build();
  }

  /**
   * Encode the provided value into the codec to be written during serialization.
   *
   * @param value the value to encode
   */
  @Override
  public void encode(BtpError value) {
    setValueAt(0, value.getErrorCode().getCode());
    setValueAt(1, value.getErrorName());
    setValueAt(2, value.getTriggeredAt());
    setValueAt(3, value.getErrorData());
    setValueAt(4, value.getSubProtocols());
  }

}
