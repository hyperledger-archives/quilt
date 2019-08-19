package org.interledger.codecs.ildcp;

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

import org.interledger.codecs.ildcp.AsnIldcpPacketCodec;
import org.interledger.codecs.ildcp.AsnIldcpRequestPacketCodec;
import org.interledger.codecs.ildcp.AsnIldcpResponseCodec;
import org.interledger.codecs.ildcp.AsnIldcpResponsePacketDataCodec;
import org.interledger.core.InterledgerPacket;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.encoding.asn.framework.CodecContextFactory;
import org.interledger.ildcp.IldcpRequestPacket;
import org.interledger.ildcp.IldcpResponse;
import org.interledger.ildcp.IldcpResponsePacket;

import java.util.Objects;

/**
 * A factory class for constructing a CodecContext that can read and write Interledger Dynamic Configuration Protocol
 * objects using ASN.1 OER encoding.
 *
 * @see "https://github.com/interledger/rfcs/blob/master/0031-dynamic-configuration-protocol/0031-dynamic-configuration-protocol.md"
 */
public class IldcpCodecContextFactory {

  /**
   * Create an instance of {@link CodecContext} that encodes and decodes BTP packets using ASN.1 OER encoding.
   *
   * @return A new instance of {@link CodecContext}.
   */
  public static CodecContext oer() {
    final CodecContext btpCodecContext = CodecContextFactory.oer();
    return register(btpCodecContext);
  }

  /**
   * Register the IL-DCP protocol codecs into the provided context.
   *
   * @param context the context to register the codecs into
   *
   * @return The supplied {@code context} with BTP codecs registered into it.
   */
  public static CodecContext register(final CodecContext context) {
    Objects.requireNonNull(context, "context must not be null");

    // For encoding this into a `data` field.
    return context
            .register(IldcpResponse.class, AsnIldcpResponseCodec::new)
            .register(InterledgerPacket.class, AsnIldcpPacketCodec::new)
            .register(IldcpRequestPacket.class, AsnIldcpRequestPacketCodec::new)
            .register(IldcpResponsePacket.class, AsnIldcpResponsePacketDataCodec::new);
  }

}
