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

import org.interledger.codecs.ilp.AsnFulfillmentCodec;
import org.interledger.encoding.asn.codecs.AsnOctetStringCodec;
import org.interledger.encoding.asn.codecs.AsnSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;
import org.interledger.ildcp.IldcpResponse;
import org.interledger.ildcp.IldcpResponsePacket;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AsnIldcpResponsePacketDataCodec extends AsnSequenceCodec<IldcpResponsePacket> {

  /**
   * Default constructor.
   */
  public AsnIldcpResponsePacketDataCodec() {
    super(
        new AsnFulfillmentCodec(),
        new AsnOctetStringCodec(new AsnSizeConstraint(0, 32768)));
  }

  @Override
  public IldcpResponsePacket decode() {
    // Decode the Data...
    final IldcpResponse ildcpResponse;
    try {
      ildcpResponse = IldcpCodecContextFactory.oer()
          .read(IldcpResponse.class, new ByteArrayInputStream(getValueAt(1)));
    } catch (IOException e) {
      throw new IldcpCodecException(e.getMessage(), e);
    }

    return IldcpResponsePacket.builder()
        .fulfillment(getValueAt(0))
        .ildcpResponse(ildcpResponse)
        .build();
  }

  @Override
  public void encode(IldcpResponsePacket value) {
    // Encode the IldcpResponse into a byte-array for placement into the `data` property.
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    try {
      IldcpCodecContextFactory.oer().write(value.getIldcpResponse(), os);
    } catch (IOException e) {
      throw new IldcpCodecException(e.getMessage(), e);
    }

    setValueAt(0, value.getFulfillment());
    setValueAt(1, os.toByteArray());
  }
}
