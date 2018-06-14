package org.interledger.ildcp.asn.framework;

/*-
 * ========================LICENSE_START=================================
 * Interledger Dynamic Configuration Protocol Core Codecs
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

import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.ildcp.IldcpResponse;
import org.interledger.ildcp.asn.codecs.AsnIldcpResponseCodec;

public class IldcpCodecs {

  public static void register(CodecContext context) {
    context.register(IldcpResponse.class, AsnIldcpResponseCodec::new);
  }

}
