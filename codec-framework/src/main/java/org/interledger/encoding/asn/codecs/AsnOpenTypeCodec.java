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

import org.interledger.encoding.asn.framework.AsnObjectCodec;

/**
 * An ASN.1 codec for UInt8 objects that decodes them into {@link Integer} values.
 */
public class AsnOpenTypeCodec<T> extends AsnObjectCodecBase<T> {

  private AsnObjectCodec<T> innerCodec;

  public AsnOpenTypeCodec(AsnObjectCodec<T> innerCodec) {
    this.innerCodec = innerCodec;
  }


  @Override
  public T decode() {
    return innerCodec.decode();
  }

  @Override
  public void encode(T value) {
    this.innerCodec.encode(value);
    this.onValueChangedEvent();
  }

  public AsnObjectCodec<T> getInnerCodec() {
    return innerCodec;
  }
}
