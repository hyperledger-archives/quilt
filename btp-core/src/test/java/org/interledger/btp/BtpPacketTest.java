package org.interledger.btp;

/*-
 * ========================LICENSE_START=================================
 * Bilateral Transfer Protocol Core Libs
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.interledger.btp.BtpSubProtocol.ContentType;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class BtpPacketTest {

  private static final long REQUEST_ID = 1;
  private static final BtpSubProtocols SUB_PROTOCOLS = new BtpSubProtocols();
  private static final BtpSubProtocol SUB_PROTOCOL_1 = BtpSubProtocol.builder()
      .protocolName("TEST")
      .contentType(ContentType.MIME_TEXT_PLAIN_UTF8)
      .data("Test Data".getBytes(StandardCharsets.UTF_8))
      .build();

  private static final BtpSubProtocol SUB_PROTOCOL_2 = BtpSubProtocol.builder()
      .protocolName("TEST2")
      .contentType(ContentType.MIME_TEXT_PLAIN_UTF8)
      .data("Test Data".getBytes(StandardCharsets.UTF_8))
      .build();

  static {
    SUB_PROTOCOLS.add(SUB_PROTOCOL_1);
    SUB_PROTOCOLS.add(SUB_PROTOCOL_2);
  }

  @Test
  public void getSubProtocols() {
    final BtpMessage message = BtpMessage.builder()
        .requestId(REQUEST_ID)
        .subProtocols(SUB_PROTOCOLS)
        .build();

    assertEquals(message.getSubProtocols(), SUB_PROTOCOLS);
    assertEquals(message.getSubProtocols().size(), 2);
  }

  @Test
  public void getPrimarySubProtocol() {
    final BtpMessage message = BtpMessage.builder()
        .requestId(REQUEST_ID)
        .subProtocols(SUB_PROTOCOLS)
        .build();

    assertEquals(message.getPrimarySubProtocol(), SUB_PROTOCOL_1);
  }

  @Test
  public void getSubProtocol() {
    final BtpMessage message = BtpMessage.builder()
        .requestId(REQUEST_ID)
        .subProtocols(SUB_PROTOCOLS)
        .build();

    assertEquals(message.getSubProtocol("TEST").get(), SUB_PROTOCOL_1);
    assertEquals(message.getSubProtocol("TEST2").get(), SUB_PROTOCOL_2);
  }

  @Test
  public void hasSubProtocol() {
    final BtpMessage message = BtpMessage.builder()
        .requestId(REQUEST_ID)
        .subProtocols(SUB_PROTOCOLS)
        .build();

    assertTrue(message.hasSubProtocol("TEST"));
    assertTrue(message.hasSubProtocol("TEST2"));
    assertFalse(message.hasSubProtocol("OTHER"));

  }
}
