package org.interledger.codecs.btp;

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

import org.interledger.btp.BtpError;
import org.interledger.btp.BtpMessage;
import org.interledger.btp.BtpPacket;
import org.interledger.btp.BtpResponse;
import org.interledger.btp.BtpSubProtocol;
import org.interledger.btp.BtpSubProtocols;
import org.interledger.btp.BtpTransfer;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.encoding.asn.framework.CodecContextFactory;

import java.time.Instant;
import java.util.Objects;

/**
 * A factory class for constructing a CodecContext that can read and write Bilateral Transfer Protocol (BTP) 2.0 objects
 * using ASN.1 OER encoding.
 *
 * @see "https://github.com/interledger/rfcs/blob/master/0023-bilateral-transfer-protocol/0023-bilateral-transfer-protocol.md"
 */
public class BtpCodecContextFactory {

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
   * Register the BTP protocol codecs into the provided context.
   *
   * @param context the context to register the codecs into
   *
   * @return The supplied {@code context} with BTP codecs registered into it.
   */
  public static CodecContext register(final CodecContext context) {
    Objects.requireNonNull(context, "context must not be null");

    return context
        .register(BtpError.class, AsnBtpErrorCodec::new)
        .register(BtpMessage.class, AsnBtpMessageCodec::new)
        .register(BtpPacket.class, AsnBtpPacketCodec::new)
        .register(BtpResponse.class, AsnBtpResponseCodec::new)
        .register(BtpSubProtocol.class, AsnBtpSubProtocolCodec::new)
        .register(BtpSubProtocols.class, AsnBtpSubProtocolsCodec::new)
        .register(BtpTransfer.class, AsnBtpTransferCodec::new)
        .register(Instant.class, AsnBtpGeneralizedTimeCodec::new);
  }
}
