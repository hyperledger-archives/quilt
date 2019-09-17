package org.interledger.codecs.ildcp;

/*-
 * ========================LICENSE_START=================================
 * Interledger Dynamic Configuration Protocol Core Codecs
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

import org.interledger.codecs.ilp.AsnInterledgerAddressCodec;
import org.interledger.encoding.asn.codecs.AsnSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;
import org.interledger.encoding.asn.codecs.AsnUint8Codec;
import org.interledger.encoding.asn.codecs.AsnUtf8StringCodec;
import org.interledger.ildcp.IldcpResponse;

public class AsnIldcpResponseCodec extends AsnSequenceCodec<IldcpResponse> {

  /**
   * Default constructor.
   */
  public AsnIldcpResponseCodec() {
    super(
        new AsnInterledgerAddressCodec(),
        new AsnUint8Codec(),
        new AsnUtf8StringCodec(AsnSizeConstraint.UNCONSTRAINED)
    );
  }

  /**
   * Decode and return the value read into the codec during serialization.
   *
   * @return the decoded object
   */
  @Override
  public IldcpResponse decode() {
    return IldcpResponse.builder()
        .clientAddress(getValueAt(0))
        .assetScale(getValueAt(1))
        .assetCode(getValueAt(2))
        .build();
  }

  /**
   * Encode the provided value into the codec to be written during serialization.
   *
   * @param value the value to encode
   */
  @Override
  public void encode(IldcpResponse value) {
    setValueAt(0, value.getClientAddress());
    setValueAt(1, value.getAssetScale());
    setValueAt(2, value.getAssetCode());
  }
}
