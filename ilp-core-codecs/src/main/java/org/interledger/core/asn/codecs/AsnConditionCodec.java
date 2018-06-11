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

import org.interledger.core.Condition;
import org.interledger.encoding.asn.codecs.AsnOctetStringBasedObjectCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;

public class AsnConditionCodec extends AsnOctetStringBasedObjectCodec<Condition> {

  public AsnConditionCodec() {
    super(new AsnSizeConstraint(32));
  }

  @Override
  public Condition decode() {
    return Condition.of(getBytes());
  }

  @Override
  public void encode(Condition value) {
    setBytes(value.getHash());
  }
}
