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

import org.interledger.btp.BtpSubProtocol.ContentType;

import org.junit.Test;

import java.math.BigInteger;

import java.nio.charset.StandardCharsets;

public class BtpTransferTest {

  private static final long REQUEST_ID = 1;
  private static final BtpSubProtocols SUB_PROTOCOLS = new BtpSubProtocols();
  private static final BtpSubProtocol SUB_PROTOCOL = BtpSubProtocol.builder()
      .protocolName("TEST")
      .contentType(ContentType.MIME_TEXT_PLAIN_UTF8)
      .data("Test Data".getBytes(StandardCharsets.UTF_8))
      .build();

  static {
    SUB_PROTOCOLS.add(SUB_PROTOCOL);
  }

  @Test
  public void getType() {

    final BtpTransfer transfer = BtpTransfer.builder()
        .requestId(REQUEST_ID)
        .amount(BigInteger.TEN)
        .subProtocols(SUB_PROTOCOLS)
        .build();

    assertEquals(transfer.getType(), BtpMessageType.TRANSFER);
  }

  @Test
  public void builder() {

    final BtpTransfer transfer = BtpTransfer.builder()
        .requestId(REQUEST_ID)
        .amount(BigInteger.TEN)
        .subProtocols(SUB_PROTOCOLS)
        .build();

    assertEquals(transfer.getAmount(), BigInteger.TEN);
    assertEquals(transfer.getPrimarySubProtocol(), SUB_PROTOCOL);

  }

}
