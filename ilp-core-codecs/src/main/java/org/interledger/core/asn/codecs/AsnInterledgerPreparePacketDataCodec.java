package org.interledger.core.asn.codecs;

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

import org.interledger.core.InterledgerPreparePacket;
import org.interledger.encoding.asn.codecs.AsnOctetStringCodec;
import org.interledger.encoding.asn.codecs.AsnSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;
import org.interledger.encoding.asn.codecs.AsnUint64Codec;

public class AsnInterledgerPreparePacketDataCodec
    extends AsnSequenceCodec<InterledgerPreparePacket> {

  /**
   * Default constructor.
   */
  public AsnInterledgerPreparePacketDataCodec() {
    super(
        new AsnUint64Codec(),
        new AsnTimestampCodec(),
        new AsnConditionCodec(),
        new AsnInterledgerAddressCodec(),
        new AsnOctetStringCodec(new AsnSizeConstraint(0, 32768))  //TODO Implement max getLength
    );
  }

  @Override
  public InterledgerPreparePacket decode() {
    return InterledgerPreparePacket.builder()
        .amount(getValueAt(0))
        .expiresAt(getValueAt(1))
        .executionCondition(getValueAt(2))
        .destination(getValueAt(3))
        .data(getValueAt(4))
        .build();
  }

  @Override
  public void encode(InterledgerPreparePacket value) {
    setValueAt(0, value.getAmount());
    setValueAt(1, value.getExpiresAt());
    setValueAt(2, value.getExecutionCondition());
    setValueAt(3, value.getDestination());
    setValueAt(4, value.getData());
  }

}
