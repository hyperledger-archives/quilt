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

import static org.junit.Assert.*;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class BtpRuntimeExceptionTest {
  private static final long REQUEST_ID = 1;
  private static final String ERROR_NAME = "Test Error";
  private static final byte[] ERROR_DATA = new byte[] {0, 1, 2};
  private static final Instant TRIGGERED_AT = Instant.now();
  private static final BtpSubProtocols SUB_PROTOCOLS = new BtpSubProtocols();

  static {
    SUB_PROTOCOLS.add(BtpSubProtocol.builder()
        .protocolName("TEST")
        .contentType(BtpSubProtocolContentType.MIME_TEXT_PLAIN_UTF8)
        .data("Test Data".getBytes(StandardCharsets.UTF_8))
        .build());
  }

  @Test
  public void toBtpError() {

    BtpRuntimeException exception = new BtpRuntimeException(BtpErrorCode.F00_NotAcceptedError, ERROR_NAME);
    BtpError error = exception.toBtpError(REQUEST_ID);

    assertEquals(error.getErrorCode(), BtpErrorCode.F00_NotAcceptedError);
    assertEquals(error.getErrorName(), ERROR_NAME);
    assertEquals(error.getTriggeredAt(), exception.getTriggeredAt());

    assertTrue(new String(error.getErrorData()).startsWith("org.interledger.btp.BtpRuntimeException: Test Error"));

  }

  @Test
  public void toBtpErrorWithSubProtocols() {

    BtpRuntimeException exception = new BtpRuntimeException(BtpErrorCode.F00_NotAcceptedError, ERROR_NAME);
    BtpError error = exception.toBtpError(REQUEST_ID, SUB_PROTOCOLS);

    assertEquals(error.getErrorCode(), BtpErrorCode.F00_NotAcceptedError);
    assertEquals(error.getErrorName(), ERROR_NAME);
    assertEquals(error.getTriggeredAt(), exception.getTriggeredAt());

    assertTrue(new String(error.getErrorData()).startsWith("org.interledger.btp.BtpRuntimeException: Test Error"));

    assertEquals(error.getSubProtocols(), SUB_PROTOCOLS);
  }
}
