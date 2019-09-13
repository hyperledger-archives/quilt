package org.interledger.codecs.ildcp.utils;

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

import org.interledger.codecs.ildcp.IldcpCodecException;
import org.interledger.codecs.ildcp.IldcpUtils;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;

import org.interledger.ildcp.IldcpResponse;
import org.interledger.ildcp.IldcpResponsePacket;
import org.junit.Assert;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link IldcpUtils}.
 */
public class IldcpUtilsTest {

  private static final InterledgerAddress FOO_ADDRESS = InterledgerAddress.of("example.foo");
  private static final String BTC = "BTC";

  private IldcpResponse ILDCP_RESPONSE = IldcpResponse.builder()
      .clientAddress(FOO_ADDRESS)
      .assetScale((short) 9)
      .assetCode(BTC)
      .build();

  @Test
  public void fromIldcpResponse() {
    final IldcpResponsePacket expectedResponse = IldcpResponsePacket.builder()
        .ildcpResponse(ILDCP_RESPONSE)
        .build();

    final IldcpResponsePacket packet = IldcpUtils.fromIldcpResponse(ILDCP_RESPONSE);
    assertThat(packet.getFulfillment()).isEqualTo(expectedResponse.getFulfillment());
    assertThat(packet.getIldcpResponse()).isEqualTo(ILDCP_RESPONSE);
  }

  @Test
  public void toIldcpResponse() {
    final IldcpResponsePacket responsePacket = IldcpUtils.fromIldcpResponse(ILDCP_RESPONSE);
    final InterledgerFulfillPacket fulfillPacket = IldcpResponsePacket.builder()
        .ildcpResponse(ILDCP_RESPONSE)
        .data(responsePacket.getData())
        .build();
    final IldcpResponse actualResponse = IldcpUtils.toIldcpResponse(fulfillPacket);
    assertThat(actualResponse).isEqualTo(ILDCP_RESPONSE);
  }

  @Test(expected = IldcpCodecException.class)
  public void toIldcpResponseWithNonZeroButInvalidData() {
    try {
      final IldcpResponsePacket packet = IldcpResponsePacket.builder()
          .ildcpResponse(ILDCP_RESPONSE).data(new byte[1]).build();
      IldcpUtils.toIldcpResponse(packet);
      Assert.fail();
    } catch (IldcpCodecException e) {
      assertThat(e.getMessage()).isEqualTo("Packet must have a data payload containing an encoded instance of IldcpResponse");
      throw e;
    }
  }

  @Test(expected = IldcpCodecException.class)
  public void toIldcpResponseWithNoData() {
    try {
      final InterledgerFulfillPacket fulfillmentWithoutData = InterledgerFulfillPacket.builder()
          .fulfillment(InterledgerFulfillment.of(new byte[32]))
          .build();
      IldcpUtils.toIldcpResponse(fulfillmentWithoutData);
    } catch (IldcpCodecException e) {
      assertThat(e.getMessage()).isEqualTo("Packet must have a data payload containing an encoded instance of IldcpResponse");
      throw e;
    }
  }
}
