package org.interledger.core;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.interledger.core.InterledgerErrorCode.ErrorFamily;

import org.junit.Test;

/**
 * Unit tests for {@link InterledgerErrorCode}.
 */
public class InterledgerErrorCodeTest {

  @Test
  public void of() {
    // F00_BAD_REQUEST
    assertThat(InterledgerErrorCode.F00_BAD_REQUEST.getCode(), is("F00"));
    assertThat(InterledgerErrorCode.F00_BAD_REQUEST.getErrorFamily(), is(ErrorFamily.FINAL));
    assertThat(InterledgerErrorCode.F00_BAD_REQUEST.getName(), is("BAD REQUEST"));

    // F01_INVALID_PACKET
    assertThat(InterledgerErrorCode.F01_INVALID_PACKET.getCode(), is("F01"));
    assertThat(InterledgerErrorCode.F01_INVALID_PACKET.getErrorFamily(), is(ErrorFamily.FINAL));
    assertThat(InterledgerErrorCode.F01_INVALID_PACKET.getName(), is("INVALID PACKET"));

    // F02_UNREACHABLE
    assertThat(InterledgerErrorCode.F02_UNREACHABLE.getCode(), is("F02"));
    assertThat(InterledgerErrorCode.F02_UNREACHABLE.getErrorFamily(), is(ErrorFamily.FINAL));
    assertThat(InterledgerErrorCode.F02_UNREACHABLE.getName(), is("UNREACHABLE"));

    // F03_INVALID_AMOUNT
    assertThat(InterledgerErrorCode.F03_INVALID_AMOUNT.getCode(), is("F03"));
    assertThat(InterledgerErrorCode.F03_INVALID_AMOUNT.getErrorFamily(), is(ErrorFamily.FINAL));
    assertThat(InterledgerErrorCode.F03_INVALID_AMOUNT.getName(), is("INVALID AMOUNT"));

    // F04_INSUFFICIENT_DST_AMOUNT
    assertThat(InterledgerErrorCode.F04_INSUFFICIENT_DST_AMOUNT.getCode(), is("F04"));
    assertThat(InterledgerErrorCode.F04_INSUFFICIENT_DST_AMOUNT.getErrorFamily(), is(ErrorFamily.FINAL));
    assertThat(InterledgerErrorCode.F04_INSUFFICIENT_DST_AMOUNT.getName(), is("INSUFFICIENT DESTINATION AMOUNT"));

    // F05_WRONG_CONDITION
    assertThat(InterledgerErrorCode.F05_WRONG_CONDITION.getCode(), is("F05"));
    assertThat(InterledgerErrorCode.F05_WRONG_CONDITION.getErrorFamily(), is(ErrorFamily.FINAL));
    assertThat(InterledgerErrorCode.F05_WRONG_CONDITION.getName(), is("WRONG CONDITION"));

    // F06_UNEXPECTED_PAYMENT
    assertThat(InterledgerErrorCode.F06_UNEXPECTED_PAYMENT.getCode(), is("F06"));
    assertThat(InterledgerErrorCode.F06_UNEXPECTED_PAYMENT.getErrorFamily(), is(ErrorFamily.FINAL));
    assertThat(InterledgerErrorCode.F06_UNEXPECTED_PAYMENT.getName(), is("UNEXPECTED PAYMENT"));

    // F07_CANNOT_RECEIVE
    assertThat(InterledgerErrorCode.F07_CANNOT_RECEIVE.getCode(), is("F07"));
    assertThat(InterledgerErrorCode.F07_CANNOT_RECEIVE.getErrorFamily(), is(ErrorFamily.FINAL));
    assertThat(InterledgerErrorCode.F07_CANNOT_RECEIVE.getName(), is("CANNOT RECEIVE"));

    // F08_AMOUNT_TOO_LARGE
    assertThat(InterledgerErrorCode.F08_AMOUNT_TOO_LARGE.getCode(), is("F08"));
    assertThat(InterledgerErrorCode.F08_AMOUNT_TOO_LARGE.getErrorFamily(), is(ErrorFamily.FINAL));
    assertThat(InterledgerErrorCode.F08_AMOUNT_TOO_LARGE.getName(), is("AMOUNT TOO LARGE"));

    // F99_APPLICATION_ERROR
    assertThat(InterledgerErrorCode.F99_APPLICATION_ERROR.getCode(), is("F99"));
    assertThat(InterledgerErrorCode.F99_APPLICATION_ERROR.getErrorFamily(), is(ErrorFamily.FINAL));
    assertThat(InterledgerErrorCode.F99_APPLICATION_ERROR.getName(), is("APPLICATION ERROR"));

    // T00_BAD_REQUEST
    assertThat(InterledgerErrorCode.T00_INTERNAL_ERROR.getCode(), is("T00"));
    assertThat(InterledgerErrorCode.T00_INTERNAL_ERROR.getErrorFamily(), is(ErrorFamily.TEMPORARY));
    assertThat(InterledgerErrorCode.T00_INTERNAL_ERROR.getName(), is("INTERNAL ERROR"));

    // T01_PEER_UNREACHABLE
    assertThat(InterledgerErrorCode.T01_PEER_UNREACHABLE.getCode(), is("T01"));
    assertThat(InterledgerErrorCode.T01_PEER_UNREACHABLE.getErrorFamily(), is(ErrorFamily.TEMPORARY));
    assertThat(InterledgerErrorCode.T01_PEER_UNREACHABLE.getName(), is("PEER UNREACHABLE"));

    // T02_PEER_BUSY
    assertThat(InterledgerErrorCode.T02_PEER_BUSY.getCode(), is("T02"));
    assertThat(InterledgerErrorCode.T02_PEER_BUSY.getErrorFamily(), is(ErrorFamily.TEMPORARY));
    assertThat(InterledgerErrorCode.T02_PEER_BUSY.getName(), is("PEER BUSY"));

    // T03_CONNECTOR_BUSY
    assertThat(InterledgerErrorCode.T03_CONNECTOR_BUSY.getCode(), is("T03"));
    assertThat(InterledgerErrorCode.T03_CONNECTOR_BUSY.getErrorFamily(), is(ErrorFamily.TEMPORARY));
    assertThat(InterledgerErrorCode.T03_CONNECTOR_BUSY.getName(), is("CONNECTOR BUSY"));

    // T04_INSUFFICIENT_LIQUIDITY
    assertThat(InterledgerErrorCode.T04_INSUFFICIENT_LIQUIDITY.getCode(), is("T04"));
    assertThat(InterledgerErrorCode.T04_INSUFFICIENT_LIQUIDITY.getErrorFamily(), is(ErrorFamily.TEMPORARY));
    assertThat(InterledgerErrorCode.T04_INSUFFICIENT_LIQUIDITY.getName(), is("INSUFFICIENT LIQUIDITY"));

    // T05_RATE_LIMITED
    assertThat(InterledgerErrorCode.T05_RATE_LIMITED.getCode(), is("T05"));
    assertThat(InterledgerErrorCode.T05_RATE_LIMITED.getErrorFamily(), is(ErrorFamily.TEMPORARY));
    assertThat(InterledgerErrorCode.T05_RATE_LIMITED.getName(), is("RATE LIMITED"));

    // T99_APPLICATION_ERROR
    assertThat(InterledgerErrorCode.T99_APPLICATION_ERROR.getCode(), is("T99"));
    assertThat(InterledgerErrorCode.T99_APPLICATION_ERROR.getErrorFamily(), is(ErrorFamily.TEMPORARY));
    assertThat(InterledgerErrorCode.T99_APPLICATION_ERROR.getName(), is("APPLICATION ERROR"));

    // R00_TRANSFER_TIMED_OUT
    assertThat(InterledgerErrorCode.R00_TRANSFER_TIMED_OUT.getCode(), is("R00"));
    assertThat(InterledgerErrorCode.R00_TRANSFER_TIMED_OUT.getErrorFamily(), is(ErrorFamily.RELATIVE));
    assertThat(InterledgerErrorCode.R00_TRANSFER_TIMED_OUT.getName(), is("TRANSFER TIMED OUT"));

    // R01_INSUFFICIENT_SOURCE_AMOUNT
    assertThat(InterledgerErrorCode.R01_INSUFFICIENT_SOURCE_AMOUNT.getCode(), is("R01"));
    assertThat(InterledgerErrorCode.R01_INSUFFICIENT_SOURCE_AMOUNT.getErrorFamily(), is(ErrorFamily.RELATIVE));
    assertThat(InterledgerErrorCode.R01_INSUFFICIENT_SOURCE_AMOUNT.getName(), is("INSUFFICIENT SOURCE AMOUNT"));

    // R02_INSUFFICIENT_TIMEOUT
    assertThat(InterledgerErrorCode.R02_INSUFFICIENT_TIMEOUT.getCode(), is("R02"));
    assertThat(InterledgerErrorCode.R02_INSUFFICIENT_TIMEOUT.getErrorFamily(), is(ErrorFamily.RELATIVE));
    assertThat(InterledgerErrorCode.R02_INSUFFICIENT_TIMEOUT.getName(), is("INSUFFICIENT TIMEOUT"));

    // R99_APPLICATION_ERROR
    assertThat(InterledgerErrorCode.R99_APPLICATION_ERROR.getCode(), is("R99"));
    assertThat(InterledgerErrorCode.R99_APPLICATION_ERROR.getErrorFamily(), is(ErrorFamily.RELATIVE));
    assertThat(InterledgerErrorCode.R99_APPLICATION_ERROR.getName(), is("APPLICATION ERROR"));

  }

  @Test
  public void valueOf() {
    assertThat(InterledgerErrorCode.valueOf("F00"), is(InterledgerErrorCode.F00_BAD_REQUEST));
    assertThat(InterledgerErrorCode.valueOf("F01"), is(InterledgerErrorCode.F01_INVALID_PACKET));
    assertThat(InterledgerErrorCode.valueOf("F02"), is(InterledgerErrorCode.F02_UNREACHABLE));
    assertThat(InterledgerErrorCode.valueOf("F03"), is(InterledgerErrorCode.F03_INVALID_AMOUNT));
    assertThat(InterledgerErrorCode.valueOf("F04"), is(InterledgerErrorCode.F04_INSUFFICIENT_DST_AMOUNT));
    assertThat(InterledgerErrorCode.valueOf("F05"), is(InterledgerErrorCode.F05_WRONG_CONDITION));
    assertThat(InterledgerErrorCode.valueOf("F06"), is(InterledgerErrorCode.F06_UNEXPECTED_PAYMENT));
    assertThat(InterledgerErrorCode.valueOf("F07"), is(InterledgerErrorCode.F07_CANNOT_RECEIVE));
    assertThat(InterledgerErrorCode.valueOf("F08"), is(InterledgerErrorCode.F08_AMOUNT_TOO_LARGE));
    assertThat(InterledgerErrorCode.valueOf("F99"), is(InterledgerErrorCode.F99_APPLICATION_ERROR));

    assertThat(InterledgerErrorCode.valueOf("T00"), is(InterledgerErrorCode.T00_INTERNAL_ERROR));
    assertThat(InterledgerErrorCode.valueOf("T01"), is(InterledgerErrorCode.T01_PEER_UNREACHABLE));
    assertThat(InterledgerErrorCode.valueOf("T02"), is(InterledgerErrorCode.T02_PEER_BUSY));
    assertThat(InterledgerErrorCode.valueOf("T03"), is(InterledgerErrorCode.T03_CONNECTOR_BUSY));
    assertThat(InterledgerErrorCode.valueOf("T04"), is(InterledgerErrorCode.T04_INSUFFICIENT_LIQUIDITY));
    assertThat(InterledgerErrorCode.valueOf("T05"), is(InterledgerErrorCode.T05_RATE_LIMITED));
    assertThat(InterledgerErrorCode.valueOf("T99"), is(InterledgerErrorCode.T99_APPLICATION_ERROR));

    assertThat(InterledgerErrorCode.valueOf("R00"), is(InterledgerErrorCode.R00_TRANSFER_TIMED_OUT));
    assertThat(InterledgerErrorCode.valueOf("R01"), is(InterledgerErrorCode.R01_INSUFFICIENT_SOURCE_AMOUNT));
    assertThat(InterledgerErrorCode.valueOf("R02"), is(InterledgerErrorCode.R02_INSUFFICIENT_TIMEOUT));
    assertThat(InterledgerErrorCode.valueOf("R99"), is(InterledgerErrorCode.R99_APPLICATION_ERROR));
  }
}