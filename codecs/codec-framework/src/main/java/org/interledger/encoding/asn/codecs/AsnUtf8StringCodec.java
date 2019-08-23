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
 * An ASN.1 codec for UTF8String objects.
 */
public class AsnUtf8StringCodec extends AsnUtf8StringBasedObjectCodec<String> {

  public AsnUtf8StringCodec(AsnSizeConstraint sizeConstraint) {
    super(sizeConstraint);
  }

  public AsnUtf8StringCodec(int fixedSizeConstraint) {
    super(fixedSizeConstraint);
  }

  public AsnUtf8StringCodec(int minSize, int maxSize) {
    super(minSize, maxSize);
  }

  @Override
  public String decode() {
    return getCharString();
  }

  @Override
  public void encode(String value) {
    setCharString(value);
  }

  @Override
  public String toString() {
    return "AsnUtf8StringCodec{"
        + "value='" + decode() + '\''
        + '}';
  }
}
