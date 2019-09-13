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

import org.interledger.codecs.ilp.AsnConditionCodec;
import org.interledger.codecs.ilp.AsnFulfillmentCodec;
import org.interledger.codecs.ilp.AsnInterledgerAddressCodec;
import org.interledger.codecs.ilp.AsnInterledgerAddressPrefixCodec;
import org.interledger.codecs.ilp.AsnInterledgerErrorCodeCodec;
import org.interledger.codecs.ilp.AsnInterledgerFulfillPacketCodec;
import org.interledger.codecs.ilp.AsnInterledgerPacketCodec;
import org.interledger.codecs.ilp.AsnInterledgerPreparePacketCodec;
import org.interledger.codecs.ilp.AsnInterledgerRejectPacketCodec;
import org.interledger.codecs.ilp.AsnTimestampCodec;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;

import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.encoding.asn.framework.CodecContextFactory;
import org.interledger.encoding.asn.serializers.oer.AsnCharStringOerSerializer;
import org.interledger.encoding.asn.serializers.oer.AsnOctetStringOerSerializer;
import org.interledger.encoding.asn.serializers.oer.AsnSequenceOerSerializer;

import java.time.Instant;
import java.util.Objects;

/**
 * A factory class for constructing a CodecContext that can read and write Interledger objects using ASN.1 OER
 * encoding.
 */
public class InterledgerCodecContextFactory {

  /**
   * Create an instance of {@link CodecContext} that encodes and decodes Interledger packets using ASN.1 OER encoding.
   *
   * @return A new instance of {@link CodecContext}.
   */
  public static CodecContext oer() {
    final CodecContext ilpCodecContext = CodecContextFactory.oer();
    return register(ilpCodecContext);
  }

  /**
   * Register the ILP codecs into the provided context.
   *
   * @param context the context to register the codecs into
   * @return The supplied {@code context} with ILP Codecs registered into it.
   */
  public static CodecContext register(final CodecContext context) {
    Objects.requireNonNull(context, "context must not be null");

    return context
        .register(Instant.class, AsnTimestampCodec::new)
        .register(InterledgerCondition.class, AsnConditionCodec::new, new AsnOctetStringOerSerializer())
        .register(InterledgerFulfillment.class, AsnFulfillmentCodec::new, new AsnOctetStringOerSerializer())
        .register(InterledgerAddress.class, AsnInterledgerAddressCodec::new, new AsnCharStringOerSerializer())
        .register(
            InterledgerAddressPrefix.class, AsnInterledgerAddressPrefixCodec::new, new AsnCharStringOerSerializer()
        )
        .register(InterledgerErrorCode.class, AsnInterledgerErrorCodeCodec::new, new AsnCharStringOerSerializer())
        .register(InterledgerPacket.class, AsnInterledgerPacketCodec::new, new AsnSequenceOerSerializer())
        .register(InterledgerPreparePacket.class, AsnInterledgerPreparePacketCodec::new, new AsnSequenceOerSerializer())
        .register(InterledgerFulfillPacket.class, AsnInterledgerFulfillPacketCodec::new, new AsnSequenceOerSerializer())
        .register(InterledgerRejectPacket.class, AsnInterledgerRejectPacketCodec::new, new AsnSequenceOerSerializer());
  }
}
