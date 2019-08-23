package org.interledger.codecs.ilp;

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

import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.encoding.asn.codecs.AsnOctetStringCodec;
import org.interledger.encoding.asn.codecs.AsnSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;

public class AsnInterledgerFulfillPacketDataCodec extends AsnSequenceCodec<InterledgerFulfillPacket> {

  /**
   * Default constructor.
   */
  public AsnInterledgerFulfillPacketDataCodec() {
    super(
        new AsnFulfillmentCodec(),
        new AsnOctetStringCodec(new AsnSizeConstraint(0, 32768)));
  }

  @Override
  public InterledgerFulfillPacket decode() {
    return InterledgerFulfillPacket.builder()
        .fulfillment(getValueAt(0))
        .data(getValueAt(1))
        .build();
  }

  @Override
  public void encode(InterledgerFulfillPacket value) {
    setValueAt(0, value.getFulfillment());
    setValueAt(1, value.getData());
  }

}
