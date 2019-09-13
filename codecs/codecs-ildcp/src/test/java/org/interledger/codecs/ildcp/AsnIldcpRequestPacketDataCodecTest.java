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
import org.interledger.core.InterledgerCondition;
import org.interledger.ildcp.IldcpRequestPacket;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AsnIldcpRequestPacketDataCodec}.
 */
public class AsnIldcpRequestPacketDataCodecTest {

  private static final Instant NOW = Instant.parse("2019-12-25T01:02:03.590Z");
  private AsnIldcpRequestPacketDataCodec codec;
  private IldcpRequestPacket packet;

  @Before
  public void setUp() {
    packet = IldcpRequestPacket.builder().expiresAt(NOW).build();
    codec = new AsnIldcpRequestPacketDataCodec();
  }

  @Test
  public void encode() {
    codec.encode(packet);
    assertThat((BigInteger) codec.getValueAt(0)).isEqualTo(BigInteger.ZERO); // Amount
    assertThat((Instant) codec.getValueAt(1)).isEqualTo(NOW); // Expiry
    assertThat((InterledgerCondition) codec.getValueAt(2)).isEqualTo(IldcpRequestPacket.EXECUTION_CONDITION); // Condition
    assertThat((InterledgerAddress) codec.getValueAt(3)).isEqualTo(IldcpRequestPacket.PEER_DOT_CONFIG); // Dest Address
    assertThat((byte[]) codec.getValueAt(4)).isEqualTo(new byte[0]); // Data
  }

  @Test
  public void decode() throws IOException {
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    IldcpCodecContextFactory.oer().write(packet, os);

    final IldcpRequestPacket actual = IldcpCodecContextFactory.oer()
        .read(IldcpRequestPacket.class, new ByteArrayInputStream(os.toByteArray()));
    assertThat(actual).isEqualTo(packet);
  }
}
