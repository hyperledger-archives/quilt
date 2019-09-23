package org.interledger.ildcp;

/*-
 * ========================LICENSE_START=================================
 * Interledger DCP Core
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

import org.junit.Test;

import java.math.BigInteger;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

/**
 * Unit tests for {@link IldcpRequestPacket}.
 */
public class IldcpRequestPacketTest {

  @Test
  public void testBuilder() {
    final IldcpRequestPacket actual = IldcpRequestPacket.builder().build();
    assertThat(actual.getAmount()).isEqualTo(BigInteger.ZERO);
    assertThat(actual.getDestination()).isEqualTo(IldcpRequestPacket.PEER_DOT_CONFIG);
    assertThat(actual.getExecutionCondition()).isEqualTo(IldcpRequestPacket.EXECUTION_CONDITION);
    assertThat(actual.getData()).isEqualTo(new byte[0]);
    // interface
    assertThat(spy(IldcpRequestPacket.class).getAmount()).isEqualTo(BigInteger.ZERO);
    assertThat(spy(IldcpRequestPacket.class).getExecutionCondition()).isEqualTo(IldcpRequestPacket.EXECUTION_CONDITION);
    assertThat(spy(IldcpRequestPacket.class).getDestination()).isEqualTo(IldcpRequestPacket.PEER_DOT_CONFIG);
    assertThat(spy(IldcpRequestPacket.class).getData()).isEqualTo(IldcpRequestPacket.EMPTY_DATA);
  }

  @Test
  public void testBuilderWithCustomExpiry() {
    final Instant expiresAt = Instant.parse("2019-12-25T01:02:03.996Z");
    final IldcpRequestPacket actual = IldcpRequestPacket.builder().expiresAt(expiresAt).build();

    assertThat(actual.getDestination()).isEqualTo(IldcpRequestPacket.PEER_DOT_CONFIG);
    assertThat(actual.getExpiresAt()).isEqualTo(expiresAt);
    assertThat(actual.getAmount()).isEqualTo(BigInteger.ZERO);
    assertThat(actual.getExecutionCondition()).isEqualTo(IldcpRequestPacket.EXECUTION_CONDITION);
    assertThat(actual.getData()).isEqualTo(new byte[0]);
  }

  @Test
  public void testEqualsHashcode() {
    final Instant expiresAt = Instant.parse("2019-12-25T01:02:03.996Z");
    final IldcpRequestPacket first = IldcpRequestPacket.builder().expiresAt(expiresAt).build();
    final IldcpRequestPacket second = IldcpRequestPacket.builder().expiresAt(expiresAt).build();
    final IldcpRequestPacket third = IldcpRequestPacket.builder().amount(BigInteger.TEN).build();

    assertThat(first).isEqualTo(second);
    assertThat(second).isEqualTo(first);
    assertThat(third).isNotEqualTo(first);

    assertThat(first.hashCode()).isEqualTo(second.hashCode());
    assertThat(second.hashCode()).isEqualTo(first.hashCode());
    assertThat(third).isNotEqualTo(first.hashCode());
  }

  @Test
  public void testToString() {
    final Instant expiresAt = Instant.parse("2019-12-25T01:02:03.996Z");
    final IldcpRequestPacket first = IldcpRequestPacket.builder().expiresAt(expiresAt).build();

    assertThat(
        first.toString()).startsWith(
            "IldcpRequestPacket{destination=InterledgerAddress{value=peer.config}, amount=0, executionCondition="
                + "Condition{hash=Zmh6rfhivXdsj8GLjp+OIAiXFIVu4jOzkCpZHQ1fKSU=}, expiresAt=2019-12-25T01:02:03.996Z,"
                + " data=[B@");
  }
}
