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

import org.interledger.core.InterledgerRejectPacket;
import org.interledger.encoding.asn.codecs.AsnOctetStringCodec;
import org.interledger.encoding.asn.codecs.AsnSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;
import org.interledger.encoding.asn.codecs.AsnUtf8StringCodec;

import java.util.Optional;

public class AsnInterledgerRejectPacketDataCodec extends AsnSequenceCodec<InterledgerRejectPacket> {

  /**
   * Default constructor.
   */
  public AsnInterledgerRejectPacketDataCodec() {
    super(
        new AsnInterledgerErrorCodeCodec(),
        new AsnInterledgerAddressCodec(),
        new AsnUtf8StringCodec(new AsnSizeConstraint(0, 8192)),
        new AsnOctetStringCodec(new AsnSizeConstraint(0, 32768))
    );
  }

  @Override
  public InterledgerRejectPacket decode() {
    return InterledgerRejectPacket.builder()
        .code(getValueAt(0))
        .triggeredBy(Optional.ofNullable(getValueAt(1)))
        .message(getValueAt(2))
        .data(getValueAt(3))
        .build();
  }

  @Override
  public void encode(InterledgerRejectPacket value) {
    setValueAt(0, value.getCode());
    setValueAt(1, value.getTriggeredBy().orElse(null));
    setValueAt(2, value.getMessage());
    setValueAt(3, value.getData());
  }

}
