package org.interledger.encoding.asn.codecs;

/*-
 * ========================LICENSE_START=================================
 * Interledger Codec Framework
 * %%
 * Copyright (C) 2017 - 2018 Interledger
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
 * An ASN.1 codec for UInt8 objects that decodes them into {@link Integer} values.
 */
public class AsnUint8Codec extends AsnPrimitiveCodec<Integer> {

  private Integer value;

  public AsnUint8Codec() {
    super(new AsnSizeConstraint(0,1));
  }

  @Override
  public Integer decode() {
    return value;
  }

  @Override
  public void encode(Integer value) {

    if (value > 255 || value < 0) {
      throw new IllegalArgumentException(
          "Uint8 only supports values from 0 to 255, value "
              + value + " is out of range.");
    }

    this.value = value;

    onValueChangedEvent();
  }

  @Override
  public String toString() {
    return "AsnUint8Codec{"
        + "value=" + decode()
        + '}';
  }
}
