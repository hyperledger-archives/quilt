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

import java.nio.charset.StandardCharsets;

/**
 * An ASN.1 codec for ASN.1 objects that extend UTF8String
 */
public abstract class AsnUtf8StringBasedObjectCodec<T> extends AsnCharStringBasedObjectCodec<T> {

  public AsnUtf8StringBasedObjectCodec(AsnSizeConstraint sizeConstraint) {
    super(sizeConstraint, StandardCharsets.UTF_8);
  }

  public AsnUtf8StringBasedObjectCodec(int fixedSizeConstraint) {
    super(fixedSizeConstraint, StandardCharsets.UTF_8);
  }

  public AsnUtf8StringBasedObjectCodec(int minSize, int maxSize) {
    super(minSize, maxSize, StandardCharsets.UTF_8);
  }



}
