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

import static java.lang.String.format;

import org.interledger.btp.BtpMessageType;
import org.interledger.btp.BtpPacket;

import org.interledger.encoding.asn.codecs.AsnOpenTypeCodec;
import org.interledger.encoding.asn.codecs.AsnSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnUint32Codec;
import org.interledger.encoding.asn.codecs.AsnUint8Codec;
import org.interledger.encoding.asn.framework.CodecException;

public class AsnBtpPacketCodec<T extends BtpPacket> extends AsnSequenceCodec<T> {

  /**
   * Default constructor.
   */
  public AsnBtpPacketCodec() {
    super(
        new AsnUint8Codec(),
        new AsnUint32Codec(),
        null
    );
    AsnUint32Codec asnRequestIdCodec = (AsnUint32Codec) getCodecAt(1);
    asnRequestIdCodec.setValueChangedEventListener((codec) -> onRequestIdChanged(codec.decode()));
  }

  /**
   * Decode and return the value read into the codec during serialization.
   *
   * @return the decoded object
   */
  @Override
  public T decode() {
    return getValueAt(2);
  }

  /**
   * Encode the provided value into the codec to be written during serialization.
   *
   * @param value the value to encode
   */
  @Override
  public void encode(T value) {
    setValueAt(0, value.getType().getCode());
    setValueAt(1, value.getRequestId());
    setValueAt(2, value);
  }

  protected void onRequestIdChanged(long code) {

    BtpMessageType type = BtpMessageType.fromCode(getValueAt(0));
    long requestId = getValueAt(1);

    //The packet type has been set so set the packet data
    switch (type) {
      case RESPONSE:
        setCodecAt(2, new AsnOpenTypeCodec<>(new AsnBtpResponseDataCodec(requestId)));
        return;
      case ERROR:
        setCodecAt(2, new AsnOpenTypeCodec<>(new AsnBtpErrorDataCodec(requestId)));
        return;
      case MESSAGE:
        setCodecAt(2, new AsnOpenTypeCodec<>(new AsnBtpMessageDataCodec(requestId)));
        return;
      case TRANSFER:
        setCodecAt(2, new AsnOpenTypeCodec<>(new AsnBtpTransferDataCodec(requestId)));
        return;
      default:
        throw new CodecException(
            format("Unknown Btp packet type code: %s", code));
    }

  }
}
