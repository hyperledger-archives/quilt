package org.interledger.encoding.asn;

/*-
 * ========================LICENSE_START=================================
 * Interledger Codec Framework
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

import org.interledger.encoding.MyCustomObject;
import org.interledger.encoding.asn.codecs.AsnOctetStringCodec;
import org.interledger.encoding.asn.codecs.AsnSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;
import org.interledger.encoding.asn.codecs.AsnUint16Codec;
import org.interledger.encoding.asn.codecs.AsnUint32Codec;
import org.interledger.encoding.asn.codecs.AsnUint64Codec;
import org.interledger.encoding.asn.codecs.AsnUint8Codec;
import org.interledger.encoding.asn.codecs.AsnUintCodec;
import org.interledger.encoding.asn.codecs.AsnUtf8StringCodec;

public class AsnMyCustomObjectCodec extends AsnSequenceCodec<MyCustomObject> {

  /**
   * Codec for the custom test object.
   */
  public AsnMyCustomObjectCodec() {

    super(
        new AsnUtf8StringCodec(AsnSizeConstraint.UNCONSTRAINED),
        new AsnUtf8StringCodec(4),
        new AsnUint8Codec(),
        new AsnUint16Codec(),
        new AsnUint32Codec(),
        new AsnUint64Codec(),
        new AsnOctetStringCodec(AsnSizeConstraint.UNCONSTRAINED),
        new AsnOctetStringCodec(32),
        new AsnUintCodec()
    );

  }

  @Override
  public MyCustomObject decode() {
    return MyCustomObject.builder()
        .utf8StringProperty(getValueAt(0))
        .fixedLengthUtf8StringProperty(getValueAt(1))
        .uint8Property(getValueAt(2))
        .uint16Property(getValueAt(3))
        .uint32Property(getValueAt(4))
        .uint64Property(getValueAt(5))
        .octetStringProperty(getValueAt(6))
        .fixedLengthOctetStringProperty(getValueAt(7))
        .uintProperty(getValueAt(8))
        .build();
  }

  @Override
  public void encode(MyCustomObject value) {
    setValueAt(0, value.getUtf8StringProperty());
    setValueAt(1, value.getFixedLengthUtf8StringProperty());
    setValueAt(2, value.getUint8Property());
    setValueAt(3, value.getUint16Property());
    setValueAt(4, value.getUint32Property());
    setValueAt(5, value.getUint64Property());
    setValueAt(6, value.getOctetStringProperty());
    setValueAt(7, value.getFixedLengthOctetStringProperty());
    setValueAt(8, value.getUintProperty());
  }

}
