package org.interledger.codecs.ilp;

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
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.encoding.asn.codecs.AsnIA5StringBasedObjectCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;

import java.util.Optional;

/**
 * Sometimes an ILP Address can be omitted (e.g., a Reject packet) so this Codec overtly accepts null so that this can
 * be mapped to {@link Optional#empty()} inside of {@link InterledgerRejectPacket#getTriggeredBy()} .
 */
public class AsnInterledgerAddressCodec extends AsnIA5StringBasedObjectCodec<InterledgerAddress> {

  public AsnInterledgerAddressCodec() {
    super(new AsnSizeConstraint(0, 1023));
  }

  @Override
  public InterledgerAddress decode() {
    return Optional.ofNullable(getCharString())
        // If the internal String is "", then treat this as a non-existent ILP Address
        .filter(charString -> !"".equals(charString))
        .map(InterledgerAddress::of)
        .orElse(null);
  }

  @Override
  public void encode(InterledgerAddress value) {
    setCharString(value == null ? "" : value.getValue());
  }
}
