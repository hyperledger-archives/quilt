package org.interledger.core.fluent;


import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.core.AmountTooLargeErrorData;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerRejectPacket;

import com.google.common.primitives.UnsignedLong;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit tests for {@link InterledgerPacketUtils}.
 */
public class InterledgerPacketUtilsTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void getAmountTooLargeErrorData() {
    expectedException.expect(NullPointerException.class);
    InterledgerPacketUtils.getAmountTooLargeErrorData(null);
  }

  @Test
  public void getAmountTooLargeErrorDataWithEmptyTypedData() {
    InterledgerRejectPacket rejectPackget = InterledgerRejectPacket.builder()
      .code(InterledgerErrorCode.F08_AMOUNT_TOO_LARGE)
      .triggeredBy(InterledgerAddress.of("example.foo"))
      .build();
    assertThat(InterledgerPacketUtils.getAmountTooLargeErrorData(rejectPackget)).isEmpty();
  }

  @Test
  public void getAmountTooLargeErrorDataWithTypedDataNotCorrect() {
    InterledgerRejectPacket rejectPackget = InterledgerRejectPacket.builder()
      .code(InterledgerErrorCode.F08_AMOUNT_TOO_LARGE)
      .triggeredBy(InterledgerAddress.of("example.foo"))
      .typedData(new Object())
      .build();
    assertThat(InterledgerPacketUtils.getAmountTooLargeErrorData(rejectPackget)).isEmpty();
  }

  @Test
  public void getAmountTooLargeErrorDataWithAmountTooLargeErrorData() {
    InterledgerRejectPacket rejectPackget = InterledgerRejectPacket.builder()
      .code(InterledgerErrorCode.F08_AMOUNT_TOO_LARGE)
      .triggeredBy(InterledgerAddress.of("example.foo"))
      .typedData(AmountTooLargeErrorData.builder()
        .receivedAmount(UnsignedLong.MAX_VALUE)
        .maximumAmount(UnsignedLong.ONE)
        .build())
      .build();
    assertThat(InterledgerPacketUtils.getAmountTooLargeErrorData(rejectPackget)).isPresent();
    assertThat(InterledgerPacketUtils.getAmountTooLargeErrorData(rejectPackget).get().receivedAmount())
      .isEqualTo(UnsignedLong.MAX_VALUE);
    assertThat(InterledgerPacketUtils.getAmountTooLargeErrorData(rejectPackget).get().maximumAmount())
      .isEqualTo(UnsignedLong.ONE);
  }

  @Test
  public void getAmountTooLargeErrorDataWithNoTypedDataAndIncorrectEmptyBytes() {
    InterledgerRejectPacket rejectPackget = InterledgerRejectPacket.builder()
      .code(InterledgerErrorCode.F08_AMOUNT_TOO_LARGE)
      .triggeredBy(InterledgerAddress.of("example.foo"))
      .data(new byte[1])
      .build();
    assertThat(InterledgerPacketUtils.getAmountTooLargeErrorData(rejectPackget)).isEmpty();
  }

  @Test
  public void getAmountTooLargeErrorDataWithNoTypedDataAndIncorrectBytes() {
    InterledgerRejectPacket rejectPackget = InterledgerRejectPacket.builder()
      .code(InterledgerErrorCode.F08_AMOUNT_TOO_LARGE)
      .triggeredBy(InterledgerAddress.of("example.foo"))
      .data(new byte[20])
      .build();
    assertThat(InterledgerPacketUtils.getAmountTooLargeErrorData(rejectPackget)).isPresent();
    assertThat(InterledgerPacketUtils.getAmountTooLargeErrorData(rejectPackget).get().receivedAmount())
      .isEqualTo(UnsignedLong.ZERO);
    assertThat(InterledgerPacketUtils.getAmountTooLargeErrorData(rejectPackget).get().maximumAmount())
      .isEqualTo(UnsignedLong.ZERO);
  }

}