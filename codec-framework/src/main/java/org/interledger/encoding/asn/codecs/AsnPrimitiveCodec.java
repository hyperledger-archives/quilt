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
 * A base for codecs for primitive ASN.1 types.
 */
public abstract class AsnPrimitiveCodec<T> extends AsnObjectCodecBase<T> {

  private final AsnSizeConstraint sizeConstraint;

  public AsnPrimitiveCodec(AsnSizeConstraint sizeConstraint) {
    this.sizeConstraint = sizeConstraint;
  }

  public AsnPrimitiveCodec(int fixedSizeConstraint) {
    this.sizeConstraint = new AsnSizeConstraint(fixedSizeConstraint);
  }

  public AsnPrimitiveCodec(int minSize, int maxSize) {
    this.sizeConstraint = new AsnSizeConstraint(minSize, maxSize);
  }

  public final AsnSizeConstraint getSizeConstraint() {
    return this.sizeConstraint;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }

    AsnPrimitiveCodec<?> that = (AsnPrimitiveCodec<?>) obj;

    return sizeConstraint.equals(that.sizeConstraint);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + sizeConstraint.hashCode();
    return result;
  }
}
