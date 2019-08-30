package org.interledger.encoding.asn.codecs;

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

/**
 * An ASN.1 codec for UInt32 objects that decodes them into {@link Long} values..
 */
public class AsnUint32Codec extends AsnOctetStringBasedObjectCodec<Long> {

  public AsnUint32Codec() {
    super(new AsnSizeConstraint(4));
  }

  @Override
  public Long decode() {
    byte[] bytes = getBytes();
    long value = 0;
    for (int i = 0; i <= 3; i++) {
      value <<= Byte.SIZE;
      value |= (bytes[i] & 0xFF);
    }
    return value;
  }

  @Override
  public void encode(Long value) {
    if (value > 4294967295L || value < 0) {
      throw new IllegalArgumentException(
          "Uint32 only supports values from 0 to 4294967295, value "
              + value + " is out of range.");
    }

    byte[] bytes = new byte[4];
    for (int i = 0; i <= 3; i++) {
      bytes[i] = ((byte) ((value >> (Byte.SIZE * (3 - i))) & 0xFF));
    }
    setBytes(bytes);
  }

  @Override
  public String toString() {
    return "AsnUint32Codec{"
        + "value=" + decode()
        + '}';
  }
}
