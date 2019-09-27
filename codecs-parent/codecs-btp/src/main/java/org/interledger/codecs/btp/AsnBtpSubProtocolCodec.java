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

import org.interledger.btp.BtpSubProtocol;
import org.interledger.btp.BtpSubProtocol.ContentType;
import org.interledger.encoding.asn.codecs.AsnIA5StringCodec;
import org.interledger.encoding.asn.codecs.AsnOctetStringCodec;
import org.interledger.encoding.asn.codecs.AsnSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;
import org.interledger.encoding.asn.codecs.AsnUint8Codec;

public class AsnBtpSubProtocolCodec extends AsnSequenceCodec<BtpSubProtocol> {

  /**
   * Default constructor.
   */
  public AsnBtpSubProtocolCodec() {
    super(
        new AsnIA5StringCodec(AsnSizeConstraint.UNCONSTRAINED),
        new AsnUint8Codec(),
        new AsnOctetStringCodec(AsnSizeConstraint.UNCONSTRAINED)
    );
  }

  /**
   * Decode and return the value read into the codec during serialization.
   *
   * @return the decoded object
   */
  @Override
  public BtpSubProtocol decode() {
    return BtpSubProtocol.builder()
        .protocolName(getValueAt(0))
        .contentType(ContentType.fromCode(getValueAt(1)))
        .data(getValueAt(2))
        .build();
  }

  /**
   * Encode the provided value into the codec to be written during serialization.
   *
   * @param value the value to encode
   */
  @Override
  public void encode(BtpSubProtocol value) {
    setValueAt(0, value.getProtocolName());
    setValueAt(1, value.getContentType().getCode());
    setValueAt(2, value.getData());
  }
}
