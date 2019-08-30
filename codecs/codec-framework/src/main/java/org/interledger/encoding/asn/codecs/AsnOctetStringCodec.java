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
 * An ASN.1 codec for octet string objects.
 */
public class AsnOctetStringCodec extends AsnOctetStringBasedObjectCodec<byte[]> {

  public AsnOctetStringCodec(AsnSizeConstraint sizeConstraint) {
    super(sizeConstraint);
  }

  public AsnOctetStringCodec(int fixedSizeConstraint) {
    super(fixedSizeConstraint);
  }

  public AsnOctetStringCodec(int minSize, int maxSize) {
    super(minSize, maxSize);
  }

  @Override
  public byte[] decode() {
    return getBytes();
  }

  @Override
  public void encode(byte[] value) {
    setBytes(value);
  }

}
