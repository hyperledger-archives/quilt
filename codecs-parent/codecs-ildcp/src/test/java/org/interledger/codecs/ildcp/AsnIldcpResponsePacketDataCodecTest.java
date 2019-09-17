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

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.ildcp.IldcpResponse;
import org.interledger.ildcp.IldcpResponsePacket;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AsnIldcpResponsePacketDataCodec}.
 */
public class AsnIldcpResponsePacketDataCodecTest {

  private static final InterledgerAddress FOO_ADDRESS = InterledgerAddress.of("example.foo");
  private static final String BTC = "BTC";
  private static final IldcpResponse TEST_RESPONSE = IldcpResponse.builder()
      .clientAddress(FOO_ADDRESS)
      .assetScale((short) 9)
      .assetCode(BTC)
      .build();

  private AsnIldcpResponsePacketDataCodec codec;
  private IldcpResponsePacket packet;

  @Before
  public void setUp() {
    packet = IldcpResponsePacket.builder().ildcpResponse(TEST_RESPONSE).build();
    codec = new AsnIldcpResponsePacketDataCodec();
  }

  @Test
  public void encode() {
    codec.encode(packet);

    assertThat((InterledgerFulfillment) codec.getValueAt(0)).isEqualTo(InterledgerFulfillment.of(new byte[32])); // fulfillment

    final byte[] encodedIldcpResponseBytes = codec.getValueAt(1);
    assertThat(Base64.getEncoder().encodeToString(encodedIldcpResponseBytes)).isEqualTo("C2V4YW1wbGUuZm9vCQNCVEM=");
  }

  @Test
  public void decode() throws IOException {
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    IldcpCodecContextFactory.oer().write(packet, os);

    final IldcpResponsePacket actual = IldcpCodecContextFactory.oer()
        .read(IldcpResponsePacket.class, new ByteArrayInputStream(os.toByteArray()));
    assertThat(actual).isEqualTo(packet);
  }

}
