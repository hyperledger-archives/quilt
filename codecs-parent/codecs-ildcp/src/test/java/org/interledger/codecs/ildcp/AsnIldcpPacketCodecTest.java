package org.interledger.codecs.ildcp;

/*-
 * ========================LICENSE_START=================================
 * Interledger DCP Core Codecs
 * %%
 * Copyright (C) 2017 - 2019 Hyperledger and its contributors
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

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.core.InterledgerAddress;
import org.interledger.encoding.asn.framework.CodecException;
import org.interledger.ildcp.IldcpRequestPacket;
import org.interledger.ildcp.IldcpResponse;
import org.interledger.ildcp.IldcpResponsePacket;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link AsnIldcpPacketCodec}.
 */
public class AsnIldcpPacketCodecTest {

  private static final InterledgerAddress FOO_ADDRESS = InterledgerAddress.of("example.foo");
  private static final String BTC = "BTC";

  private static final IldcpResponse RESPONSE = IldcpResponse.builder()
      .clientAddress(FOO_ADDRESS)
      .assetScale((short) 9)
      .assetCode(BTC)
      .build();

  private AsnIldcpPacketCodec codec;

  @Before
  public void setup() {
    this.codec = new AsnIldcpPacketCodec();
  }


  @Test
  public void encodeDecode() {
    final IldcpRequestPacket packet = IldcpRequestPacket.builder().build();
    codec.encode(packet);
    assertThat(codec.getCodecAt(0).decode()).isEqualTo((short) 12);
    assertThat(codec.getCodecAt(1).decode()).isEqualTo(packet);
  }

  @Test
  public void onTypeIdChangedToRequest() {
    final IldcpRequestPacket packet = IldcpRequestPacket.builder().build();
    codec.onTypeIdChanged(AsnIldcpPacketCodec.IldcpPacketTypes.REQUEST);
    codec.encode(packet);
    assertThat(codec.getCodecAt(0).decode()).isEqualTo((short) 12);
    assertThat(codec.getCodecAt(1).decode()).isEqualTo(packet);
  }

  @Test
  public void onTypeIdChangedToResponse() {
    final IldcpResponsePacket packet = IldcpResponsePacket.builder().ildcpResponse(RESPONSE).build();
    codec.onTypeIdChanged(AsnIldcpPacketCodec.IldcpPacketTypes.RESPONSE);
    codec.encode(packet);
    assertThat(codec.getCodecAt(0).decode()).isEqualTo((short) 13);
    assertThat(codec.getCodecAt(1).decode()).isEqualTo(packet);
  }

  @Test(expected = CodecException.class)
  public void onTypeIdChangedToUnsupported() {
    try {
      codec.onTypeIdChanged((short) 50);
      Assert.fail();
    } catch (CodecException e) {
      assertThat(e.getMessage()).isEqualTo("Unknown IL-DCP packet type code: 50");
      throw e;
    }
  }

}
