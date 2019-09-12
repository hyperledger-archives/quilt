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
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

/**
 * Unit test for {@link IldcpResponsePacketHandler}.
 */
public class IldcpResponsePacketHandlerTest {

  private static final InterledgerAddress FOO_ADDRESS = InterledgerAddress.of("example.foo");
  private static final String BTC = "BTC";

  private static final IldcpResponse RESPONSE = IldcpResponse.builder()
      .clientAddress(FOO_ADDRESS)
      .assetScale((short) 9)
      .assetCode(BTC)
      .build();

  private InterledgerResponsePacket fulfillPacket;
  private InterledgerResponsePacket rejectPacket;
  private InterledgerResponsePacket expiredPacket;

  @Before
  public void setup() {
    fulfillPacket = IldcpResponsePacket.builder().ildcpResponse(RESPONSE).data(new byte[32]).build();

    rejectPacket = InterledgerRejectPacket.builder().triggeredBy(InterledgerAddress.of("test.foo"))
        .code(InterledgerErrorCode.T00_INTERNAL_ERROR)
        .message("rejected!")
        .build();

    expiredPacket = InterledgerRejectPacket.builder().triggeredBy(InterledgerAddress.of("test.foo"))
        .code(InterledgerErrorCode.R00_TRANSFER_TIMED_OUT)
        .message("Timed out!")
        .build();
  }

  @Test(expected = NullPointerException.class)
  public void handleNullPacket() {
    new IldcpResponsePacketHandler() {

      @Override
      protected void handleIldcpResponsePacket(IldcpResponsePacket ildcpResponsePacket) {
        throw new RuntimeException("Should not fulfill!");
      }

      @Override
      protected void handleIldcpErrorPacket(InterledgerRejectPacket ildcpErrorPacket) {
        throw new RuntimeException("Should not reject!");
      }

    }.handle(null);
    fail("cannot handle null packet");
  }

  @Test
  public void handleFulfillPacket() {
    new IldcpResponsePacketHandler() {

      @Override
      protected void handleIldcpResponsePacket(IldcpResponsePacket ildcpResponsePacket) {
        assertThat(ildcpResponsePacket).isEqualTo(fulfillPacket);
      }

      @Override
      protected void handleIldcpErrorPacket(InterledgerRejectPacket ildcpErrorPacket) {
        throw new RuntimeException("Should not reject!");
      }

    }.handle(fulfillPacket);
  }

  @Test
  public void handleRejectPacket() {
    new IldcpResponsePacketHandler() {

      @Override
      protected void handleIldcpResponsePacket(IldcpResponsePacket ildcpResponsePacket) {
        throw new RuntimeException("Should not fulfill!");
      }

      @Override
      protected void handleIldcpErrorPacket(InterledgerRejectPacket ildcpErrorPacket) {
        assertThat(ildcpErrorPacket.getCode()).isEqualTo(InterledgerErrorCode.T00_INTERNAL_ERROR);
      }

    }.handle(rejectPacket);
  }

  @Test
  public void handleExpiredPacket() {
    new IldcpResponsePacketHandler() {

      @Override
      protected void handleIldcpResponsePacket(IldcpResponsePacket ildcpResponsePacket) {
        throw new RuntimeException("Should not fulfill!");
      }

      @Override
      protected void handleIldcpErrorPacket(InterledgerRejectPacket ildcpErrorPacket) {
        assertThat(ildcpErrorPacket.getCode()).isEqualTo(InterledgerErrorCode.R00_TRANSFER_TIMED_OUT);
      }
    }.handle(expiredPacket);
  }

}
