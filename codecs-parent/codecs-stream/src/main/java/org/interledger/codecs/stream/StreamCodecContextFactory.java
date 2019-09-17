package org.interledger.codecs.stream;

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

import org.interledger.codecs.ilp.InterledgerCodecContextFactory;
import org.interledger.codecs.stream.frame.AsnStreamFrameCodec;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.encoding.asn.serializers.oer.AsnSequenceOerSerializer;
import org.interledger.stream.AmountTooLargeErrorData;
import org.interledger.stream.StreamPacket;
import org.interledger.stream.frames.StreamFrame;

import java.util.Objects;

/**
 * A factory class for constructing a CodecContext that can read and write STREAM objects using ASN.1 OER encoding.
 */
public class StreamCodecContextFactory {

  /**
   * Create an instance of {@link CodecContext} that encodes and decodes Interledger packets using ASN.1 OER encoding.
   *
   * @return A new instance of {@link CodecContext}.
   */
  public static CodecContext oer() {
    final CodecContext streamCodecContext = InterledgerCodecContextFactory.oer();
    return register(streamCodecContext);
  }

  /**
   * Register the ILP codecs into the provided context.
   *
   * @param context the context to register the codecs into
   *
   * @return The supplied {@code context} with ILP Codecs registered into it.
   */
  public static CodecContext register(final CodecContext context) {
    Objects.requireNonNull(context, "context must not be null");

    return context
        .register(StreamFrame.class, AsnStreamFrameCodec::new, new AsnSequenceOerSerializer())
        .register(StreamPacket.class, AsnStreamPacketCodec::new, new AsnSequenceOerSerializer())
        .register(AmountTooLargeErrorData.class, AsnAmountTooLargeDataCodec::new);
  }
}
