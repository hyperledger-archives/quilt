package org.interledger.codecs.btp;

/*-
 * ========================LICENSE_START=================================
 * Bilateral Transfer Protocol Core Codecs
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

import org.interledger.btp.BtpPacket;
import org.interledger.encoding.asn.codecs.AsnSequenceCodec;
import org.interledger.encoding.asn.framework.AsnObjectCodec;

public abstract class AsnBtpPacketDataCodec<T extends BtpPacket> extends AsnSequenceCodec<T> {

  private final long requestId;

  public AsnBtpPacketDataCodec(long requestId, AsnObjectCodec... fields) {
    super(fields);
    this.requestId = requestId;
  }

  protected final long getRequestId() {
    return requestId;
  }

}
