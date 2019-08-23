package org.interledger.codecs.ildcp;

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

import static java.lang.String.format;

import org.interledger.core.InterledgerPacket;
import org.interledger.encoding.asn.codecs.AsnOpenTypeCodec;
import org.interledger.encoding.asn.codecs.AsnSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnUint8Codec;
import org.interledger.encoding.asn.framework.CodecException;
import org.interledger.ildcp.IldcpRequestPacket;
import org.interledger.ildcp.IldcpResponsePacket;

/**
 * Codec for encoding/decoding packets of type {@link InterledgerPacket} specifically for the IL-DCP use-case. Because
 * IL-DCP packets are actually just specially-formed Prepare and Fulfill packets, this Codec behaves similarly to the
 * AsnInterledgerPacketCodec, with some special handling.
 */
public class AsnIldcpPacketCodec<T extends InterledgerPacket> extends AsnSequenceCodec<T> {

  /**
   * Default constructor.
   */
  public AsnIldcpPacketCodec() {
    super(new AsnUint8Codec(), null);
    AsnUint8Codec asnTypeId = (AsnUint8Codec) getCodecAt(0);
    asnTypeId.setValueChangedEventListener((codec) -> {
      onTypeIdChanged(codec.decode());
    });
  }

  @Override
  public T decode() {
    return getValueAt(1);
  }

  @Override
  public void encode(T value) {
    if (value instanceof IldcpRequestPacket) {
      setValueAt(0, IldcpPacketTypes.REQUEST);
    } else if (value instanceof IldcpResponsePacket) {
      setValueAt(0, IldcpPacketTypes.RESPONSE);
    } else {
      throw new CodecException(String.format("Unknown IL-DCP Packet Type: %s", value.getClass().getName()));
    }

    setValueAt(1, value);
  }

  protected void onTypeIdChanged(int typeId) {
    //The packet type has been set so set the packet data
    switch (typeId) {
      case IldcpPacketTypes.REQUEST: // This is actually an ILP Prepare
        setCodecAt(1, new AsnOpenTypeCodec<>(new AsnIldcpRequestPacketDataCodec()));
        return;
      case IldcpPacketTypes.RESPONSE: // This is actually an ILP Fulfill
        setCodecAt(1, new AsnOpenTypeCodec<>(new AsnIldcpResponsePacketDataCodec()));
        return;
      default:
        throw new CodecException(format("Unknown IL-DCP packet type code: %s", typeId));
    }
  }

  // Reject is handled via normal reject serializer
  public static class IldcpPacketTypes {

    public static final short REQUEST = 12; // This is actually an ILP Prepare
    public static final short RESPONSE = 13; // This is actually an ILP Fulfill

  }
}
