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

import org.interledger.core.InterledgerAddress;
import org.interledger.encoding.asn.codecs.AsnIA5StringBasedObjectCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;

public class AsnInterledgerAddressCodec extends AsnIA5StringBasedObjectCodec<InterledgerAddress> {

  public AsnInterledgerAddressCodec() {
    super(new AsnSizeConstraint(1, 1023));
  }

  @Override
  public InterledgerAddress decode() {
    return InterledgerAddress.of(getCharString());
  }

  @Override
  public void encode(InterledgerAddress value) {
    setCharString(value.value());
  }
}
