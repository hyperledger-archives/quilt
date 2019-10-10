package org.interledger.codecs.stream;

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

import org.interledger.codecs.stream.frame.AsnStreamFramesCodec;
import org.interledger.core.InterledgerPacketType;
import org.interledger.encoding.asn.codecs.AsnSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnUint8Codec;
import org.interledger.encoding.asn.codecs.AsnUintCodecUL;
import org.interledger.stream.StreamPacket;

public class AsnStreamPacketCodec extends AsnSequenceCodec<StreamPacket> {

  /**
   * Default constructor.
   */
  @SuppressWarnings("CommentsIndentation")
  public AsnStreamPacketCodec() {
    super(
        new AsnUint8Codec(), // version
        new AsnUint8Codec(), // Ilp Packet Type
        new AsnUintCodecUL(), // sequence
        new AsnUintCodecUL(), // PrepareAmount
        new AsnStreamFramesCodec() // Sequences of Frames
        // JunkData (Ignored)
    );
  }

  /**
   * Decode and return the value read into the codec during serialization.
   *
   * @return the decoded object
   */
  @Override
  public StreamPacket decode() {
    return StreamPacket.builder()
        // ignore version at index 0.
        .interledgerPacketType(InterledgerPacketType.fromCode(getValueAt(1)))
        .sequence(getValueAt(2))
        .prepareAmount(getValueAt(3))
        .frames(getValueAt(4))
        .build();
  }

  /**
   * Encode the provided value into the codec to be written during serialization.
   *
   * @param value the value to encode
   */
  @Override
  public void encode(StreamPacket value) {
    setValueAt(0, value.version());
    setValueAt(1, value.interledgerPacketType().getType());
    setValueAt(2, value.sequence());
    setValueAt(3, value.prepareAmount());
    setValueAt(4, value.frames());
  }
}
