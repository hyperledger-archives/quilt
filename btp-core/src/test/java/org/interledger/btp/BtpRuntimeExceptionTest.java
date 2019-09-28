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

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.btp.BtpSubProtocol.ContentType;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

/**
 * Unit tests for {@link BtpRuntimeException}.
 */
public class BtpRuntimeExceptionTest {

  private static final long REQUEST_ID = 1;
  private static final String ERROR_MESSAGE = "Test Error";
  private static final BtpSubProtocols SUB_PROTOCOLS = new BtpSubProtocols();

  static {
    SUB_PROTOCOLS.add(BtpSubProtocol.builder()
        .protocolName("TEST")
        .contentType(ContentType.MIME_TEXT_PLAIN_UTF8)
        .data("Test Data".getBytes(StandardCharsets.UTF_8))
        .build());
  }

  @Test
  public void toBtpError() {
    final BtpRuntimeException exception = new BtpRuntimeException(BtpErrorCode.F00_NotAcceptedError, ERROR_MESSAGE);
    final BtpError error = exception.toBtpError(REQUEST_ID);

    assertThat(error.getErrorCode()).isEqualTo(BtpErrorCode.F00_NotAcceptedError);
    assertThat(error.getTriggeredAt()).isEqualTo(exception.getTriggeredAt());
    assertThat(error.getErrorData().length).isEqualTo(0);
  }

  @Test
  public void toBtpErrorWithSubProtocols() {
    final BtpRuntimeException exception = new BtpRuntimeException(BtpErrorCode.F00_NotAcceptedError, ERROR_MESSAGE);
    final BtpError error = exception.toBtpError(REQUEST_ID, SUB_PROTOCOLS);

    assertThat(error.getErrorCode()).isEqualTo(BtpErrorCode.F00_NotAcceptedError);
    assertThat(error.getTriggeredAt()).isEqualTo(exception.getTriggeredAt());
    assertThat(error.getErrorData().length).isEqualTo(0);
    assertThat(error.getSubProtocols()).isEqualTo(SUB_PROTOCOLS);
  }
}
