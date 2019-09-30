package org.interledger.core;

/*-
 * ========================LICENSE_START=================================
 * Interledger Core
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

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.core.InterledgerErrorCode.ErrorFamily;

import org.junit.Test;

/**
 * Unit tests for {@link InterledgerErrorCode}.
 */
public class InterledgerErrorCodeTest {

  @Test
  public void of() {
    // F00_BAD_REQUEST
    assertThat(InterledgerErrorCode.F00_BAD_REQUEST.getCode()).isEqualTo("F00");
    assertThat(InterledgerErrorCode.F00_BAD_REQUEST.getErrorFamily()).isEqualTo(ErrorFamily.FINAL);
    assertThat(InterledgerErrorCode.F00_BAD_REQUEST.getName()).isEqualTo("BAD REQUEST");

    // F01_INVALID_PACKET
    assertThat(InterledgerErrorCode.F01_INVALID_PACKET.getCode()).isEqualTo("F01");
    assertThat(InterledgerErrorCode.F01_INVALID_PACKET.getErrorFamily()).isEqualTo(ErrorFamily.FINAL);
    assertThat(InterledgerErrorCode.F01_INVALID_PACKET.getName()).isEqualTo("INVALID PACKET");

    // F02_UNREACHABLE
    assertThat(InterledgerErrorCode.F02_UNREACHABLE.getCode()).isEqualTo("F02");
    assertThat(InterledgerErrorCode.F02_UNREACHABLE.getErrorFamily()).isEqualTo(ErrorFamily.FINAL);
    assertThat(InterledgerErrorCode.F02_UNREACHABLE.getName()).isEqualTo("UNREACHABLE");

    // F03_INVALID_AMOUNT
    assertThat(InterledgerErrorCode.F03_INVALID_AMOUNT.getCode()).isEqualTo("F03");
    assertThat(InterledgerErrorCode.F03_INVALID_AMOUNT.getErrorFamily()).isEqualTo(ErrorFamily.FINAL);
    assertThat(InterledgerErrorCode.F03_INVALID_AMOUNT.getName()).isEqualTo("INVALID AMOUNT");

    // F04_INSUFFICIENT_DST_AMOUNT
    assertThat(InterledgerErrorCode.F04_INSUFFICIENT_DST_AMOUNT.getCode()).isEqualTo("F04");
    assertThat(InterledgerErrorCode.F04_INSUFFICIENT_DST_AMOUNT.getErrorFamily()).isEqualTo(ErrorFamily.FINAL);
    assertThat(InterledgerErrorCode.F04_INSUFFICIENT_DST_AMOUNT.getName()).isEqualTo("INSUFFICIENT DESTINATION AMOUNT");

    // F05_WRONG_CONDITION
    assertThat(InterledgerErrorCode.F05_WRONG_CONDITION.getCode()).isEqualTo("F05");
    assertThat(InterledgerErrorCode.F05_WRONG_CONDITION.getErrorFamily()).isEqualTo(ErrorFamily.FINAL);
    assertThat(InterledgerErrorCode.F05_WRONG_CONDITION.getName()).isEqualTo("WRONG CONDITION");

    // F06_UNEXPECTED_PAYMENT
    assertThat(InterledgerErrorCode.F06_UNEXPECTED_PAYMENT.getCode()).isEqualTo("F06");
    assertThat(InterledgerErrorCode.F06_UNEXPECTED_PAYMENT.getErrorFamily()).isEqualTo(ErrorFamily.FINAL);
    assertThat(InterledgerErrorCode.F06_UNEXPECTED_PAYMENT.getName()).isEqualTo("UNEXPECTED PAYMENT");

    // F07_CANNOT_RECEIVE
    assertThat(InterledgerErrorCode.F07_CANNOT_RECEIVE.getCode()).isEqualTo("F07");
    assertThat(InterledgerErrorCode.F07_CANNOT_RECEIVE.getErrorFamily()).isEqualTo(ErrorFamily.FINAL);
    assertThat(InterledgerErrorCode.F07_CANNOT_RECEIVE.getName()).isEqualTo("CANNOT RECEIVE");

    // F08_AMOUNT_TOO_LARGE
    assertThat(InterledgerErrorCode.F08_AMOUNT_TOO_LARGE.getCode()).isEqualTo("F08");
    assertThat(InterledgerErrorCode.F08_AMOUNT_TOO_LARGE.getErrorFamily()).isEqualTo(ErrorFamily.FINAL);
    assertThat(InterledgerErrorCode.F08_AMOUNT_TOO_LARGE.getName()).isEqualTo("AMOUNT TOO LARGE");

    // F99_APPLICATION_ERROR
    assertThat(InterledgerErrorCode.F99_APPLICATION_ERROR.getCode()).isEqualTo("F99");
    assertThat(InterledgerErrorCode.F99_APPLICATION_ERROR.getErrorFamily()).isEqualTo(ErrorFamily.FINAL);
    assertThat(InterledgerErrorCode.F99_APPLICATION_ERROR.getName()).isEqualTo("APPLICATION ERROR");

    // T00_BAD_REQUEST
    assertThat(InterledgerErrorCode.T00_INTERNAL_ERROR.getCode()).isEqualTo("T00");
    assertThat(InterledgerErrorCode.T00_INTERNAL_ERROR.getErrorFamily()).isEqualTo(ErrorFamily.TEMPORARY);
    assertThat(InterledgerErrorCode.T00_INTERNAL_ERROR.getName()).isEqualTo("INTERNAL ERROR");

    // T01_PEER_UNREACHABLE
    assertThat(InterledgerErrorCode.T01_PEER_UNREACHABLE.getCode()).isEqualTo("T01");
    assertThat(InterledgerErrorCode.T01_PEER_UNREACHABLE.getErrorFamily()).isEqualTo(ErrorFamily.TEMPORARY);
    assertThat(InterledgerErrorCode.T01_PEER_UNREACHABLE.getName()).isEqualTo("PEER UNREACHABLE");

    // T02_PEER_BUSY
    assertThat(InterledgerErrorCode.T02_PEER_BUSY.getCode()).isEqualTo("T02");
    assertThat(InterledgerErrorCode.T02_PEER_BUSY.getErrorFamily()).isEqualTo(ErrorFamily.TEMPORARY);
    assertThat(InterledgerErrorCode.T02_PEER_BUSY.getName()).isEqualTo("PEER BUSY");

    // T03_CONNECTOR_BUSY
    assertThat(InterledgerErrorCode.T03_CONNECTOR_BUSY.getCode()).isEqualTo("T03");
    assertThat(InterledgerErrorCode.T03_CONNECTOR_BUSY.getErrorFamily()).isEqualTo(ErrorFamily.TEMPORARY);
    assertThat(InterledgerErrorCode.T03_CONNECTOR_BUSY.getName()).isEqualTo("CONNECTOR BUSY");

    // T04_INSUFFICIENT_LIQUIDITY
    assertThat(InterledgerErrorCode.T04_INSUFFICIENT_LIQUIDITY.getCode()).isEqualTo("T04");
    assertThat(InterledgerErrorCode.T04_INSUFFICIENT_LIQUIDITY.getErrorFamily()).isEqualTo(ErrorFamily.TEMPORARY);
    assertThat(InterledgerErrorCode.T04_INSUFFICIENT_LIQUIDITY.getName()).isEqualTo("INSUFFICIENT LIQUIDITY");

    // T05_RATE_LIMITED
    assertThat(InterledgerErrorCode.T05_RATE_LIMITED.getCode()).isEqualTo("T05");
    assertThat(InterledgerErrorCode.T05_RATE_LIMITED.getErrorFamily()).isEqualTo(ErrorFamily.TEMPORARY);
    assertThat(InterledgerErrorCode.T05_RATE_LIMITED.getName()).isEqualTo("RATE LIMITED");

    // T99_APPLICATION_ERROR
    assertThat(InterledgerErrorCode.T99_APPLICATION_ERROR.getCode()).isEqualTo("T99");
    assertThat(InterledgerErrorCode.T99_APPLICATION_ERROR.getErrorFamily()).isEqualTo(ErrorFamily.TEMPORARY);
    assertThat(InterledgerErrorCode.T99_APPLICATION_ERROR.getName()).isEqualTo("APPLICATION ERROR");

    // R00_TRANSFER_TIMED_OUT
    assertThat(InterledgerErrorCode.R00_TRANSFER_TIMED_OUT.getCode()).isEqualTo("R00");
    assertThat(InterledgerErrorCode.R00_TRANSFER_TIMED_OUT.getErrorFamily()).isEqualTo(ErrorFamily.RELATIVE);
    assertThat(InterledgerErrorCode.R00_TRANSFER_TIMED_OUT.getName()).isEqualTo("TRANSFER TIMED OUT");

    // R01_INSUFFICIENT_SOURCE_AMOUNT
    assertThat(InterledgerErrorCode.R01_INSUFFICIENT_SOURCE_AMOUNT.getCode()).isEqualTo("R01");
    assertThat(InterledgerErrorCode.R01_INSUFFICIENT_SOURCE_AMOUNT.getErrorFamily()).isEqualTo(ErrorFamily.RELATIVE);
    assertThat(InterledgerErrorCode.R01_INSUFFICIENT_SOURCE_AMOUNT.getName()).isEqualTo("INSUFFICIENT SOURCE AMOUNT");

    // R02_INSUFFICIENT_TIMEOUT
    assertThat(InterledgerErrorCode.R02_INSUFFICIENT_TIMEOUT.getCode()).isEqualTo("R02");
    assertThat(InterledgerErrorCode.R02_INSUFFICIENT_TIMEOUT.getErrorFamily()).isEqualTo(ErrorFamily.RELATIVE);
    assertThat(InterledgerErrorCode.R02_INSUFFICIENT_TIMEOUT.getName()).isEqualTo("INSUFFICIENT TIMEOUT");

    // R99_APPLICATION_ERROR
    assertThat(InterledgerErrorCode.R99_APPLICATION_ERROR.getCode()).isEqualTo("R99");
    assertThat(InterledgerErrorCode.R99_APPLICATION_ERROR.getErrorFamily()).isEqualTo(ErrorFamily.RELATIVE);
    assertThat(InterledgerErrorCode.R99_APPLICATION_ERROR.getName()).isEqualTo("APPLICATION ERROR");

  }

  @Test
  public void valueOf() {
    assertThat(InterledgerErrorCode.valueOf("F00")).isEqualTo(InterledgerErrorCode.F00_BAD_REQUEST);
    assertThat(InterledgerErrorCode.valueOf("F01")).isEqualTo(InterledgerErrorCode.F01_INVALID_PACKET);
    assertThat(InterledgerErrorCode.valueOf("F02")).isEqualTo(InterledgerErrorCode.F02_UNREACHABLE);
    assertThat(InterledgerErrorCode.valueOf("F03")).isEqualTo(InterledgerErrorCode.F03_INVALID_AMOUNT);
    assertThat(InterledgerErrorCode.valueOf("F04")).isEqualTo(InterledgerErrorCode.F04_INSUFFICIENT_DST_AMOUNT);
    assertThat(InterledgerErrorCode.valueOf("F05")).isEqualTo(InterledgerErrorCode.F05_WRONG_CONDITION);
    assertThat(InterledgerErrorCode.valueOf("F06")).isEqualTo(InterledgerErrorCode.F06_UNEXPECTED_PAYMENT);
    assertThat(InterledgerErrorCode.valueOf("F07")).isEqualTo(InterledgerErrorCode.F07_CANNOT_RECEIVE);
    assertThat(InterledgerErrorCode.valueOf("F08")).isEqualTo(InterledgerErrorCode.F08_AMOUNT_TOO_LARGE);
    assertThat(InterledgerErrorCode.valueOf("F99")).isEqualTo(InterledgerErrorCode.F99_APPLICATION_ERROR);

    assertThat(InterledgerErrorCode.valueOf("T00")).isEqualTo(InterledgerErrorCode.T00_INTERNAL_ERROR);
    assertThat(InterledgerErrorCode.valueOf("T01")).isEqualTo(InterledgerErrorCode.T01_PEER_UNREACHABLE);
    assertThat(InterledgerErrorCode.valueOf("T02")).isEqualTo(InterledgerErrorCode.T02_PEER_BUSY);
    assertThat(InterledgerErrorCode.valueOf("T03")).isEqualTo(InterledgerErrorCode.T03_CONNECTOR_BUSY);
    assertThat(InterledgerErrorCode.valueOf("T04")).isEqualTo(InterledgerErrorCode.T04_INSUFFICIENT_LIQUIDITY);
    assertThat(InterledgerErrorCode.valueOf("T05")).isEqualTo(InterledgerErrorCode.T05_RATE_LIMITED);
    assertThat(InterledgerErrorCode.valueOf("T99")).isEqualTo(InterledgerErrorCode.T99_APPLICATION_ERROR);

    assertThat(InterledgerErrorCode.valueOf("R00")).isEqualTo(InterledgerErrorCode.R00_TRANSFER_TIMED_OUT);
    assertThat(InterledgerErrorCode.valueOf("R01")).isEqualTo(InterledgerErrorCode.R01_INSUFFICIENT_SOURCE_AMOUNT);
    assertThat(InterledgerErrorCode.valueOf("R02")).isEqualTo(InterledgerErrorCode.R02_INSUFFICIENT_TIMEOUT);
    assertThat(InterledgerErrorCode.valueOf("R99")).isEqualTo(InterledgerErrorCode.R99_APPLICATION_ERROR);
  }
}
