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

import org.interledger.core.InterledgerAddress;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.spy;

/**
 * Unit tests for {@link IldcpResponsePacket}.
 */
public class IldcpResponsePacketTest {

  private static final InterledgerAddress FOO_ADDRESS = InterledgerAddress.of("example.foo");
  private static final String BTC = "BTC";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  final IldcpResponse RESPONSE = IldcpResponse.builder()
      .clientAddress(FOO_ADDRESS)
      .assetScale((short) 9)
      .assetCode(BTC)
      .build();

  @Test
  public void testBuilder() {
    final IldcpResponsePacket actual = IldcpResponsePacket.builder().ildcpResponse(RESPONSE).data(new byte[32]).build();

    assertThat(actual.getFulfillment()).isEqualTo(IldcpResponsePacket.EXECUTION_FULFILLMENT);
    assertThat(actual.getData()).isEqualTo(new byte[32]);
  }

  @Test
  public void defaults() {
    IldcpResponsePacket packet = IldcpResponsePacket.builder().ildcpResponse(RESPONSE).build();
    assertThat(packet.getData()).isEqualTo(new byte[0]);
    assertThat(spy(IldcpResponsePacket.class).getFulfillment()).isEqualTo(IldcpResponsePacket.EXECUTION_FULFILLMENT);
  }

  @Test
  public void testEmptyBuilder() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Cannot build IldcpResponsePacket, some of required attributes are not set [ildcpResponse]");
    IldcpResponsePacket.builder().build();
  }

  @Test
  public void testEqualsHashcode() {

    final IldcpResponsePacket first = IldcpResponsePacket.builder().ildcpResponse(RESPONSE).data(new byte[32]).build();
    final IldcpResponsePacket second = IldcpResponsePacket.builder().ildcpResponse(RESPONSE).data(new byte[32]).build();
    final IldcpResponsePacket third = IldcpResponsePacket.builder().ildcpResponse(RESPONSE).data(new byte[64]).build();

    assertThat(first).isEqualTo(second);
    assertThat(second).isEqualTo(first);
    assertThat(third).isNotEqualTo(first);

    assertThat(first.hashCode()).isEqualTo(second.hashCode());
    assertThat(second.hashCode()).isEqualTo(first.hashCode());
    assertThat(third).isNotEqualTo(first.hashCode());
  }

  @Test
  public void testToString() {
    final IldcpResponsePacket first = IldcpResponsePacket.builder().ildcpResponse(RESPONSE).data(new byte[0]).build();

    assertThat(
        first.toString())
        .isEqualTo("IldcpResponsePacket{"
            + "ildcpResponse=IldcpResponse{clientAddress=InterledgerAddress{value=example.foo}, assetCode=BTC, assetScale=9}, "
            + "fulfillment=ImmutableInterledgerFulfillment["
            + "preimage=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=, "
            + "condition=Condition{hash=Zmh6rfhivXdsj8GLjp+OIAiXFIVu4jOzkCpZHQ1fKSU=}], "
            + "data=[]}");
  }
}
