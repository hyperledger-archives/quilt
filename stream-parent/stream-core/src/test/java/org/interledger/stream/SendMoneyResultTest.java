package org.interledger.stream;

import static com.google.common.primitives.UnsignedLong.ZERO;
import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.core.InterledgerAddress;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

import java.time.Duration;

/**
 * Unit tests for {@link SendMoneyResult}.
 */
public class SendMoneyResultTest {

  @Test
  public void testBuilderWithNoSenderAddress() {
    SendMoneyResult result = SendMoneyResult.builder()
        .destinationAddress(InterledgerAddress.of("example.destination"))
        .originalAmount(ZERO)
        .amountDelivered(UnsignedLong.ONE)
        .amountSent(UnsignedLong.valueOf(2))
        .amountLeftToSend(UnsignedLong.valueOf(3))
        .numFulfilledPackets(10)
        .numRejectPackets(5)
        .sendMoneyDuration(Duration.ofMillis(1000))
        .successfulPayment(true)
        .build();
    assertThat(result.senderAddress()).isEmpty();
    assertThat(result.destinationAddress()).isEqualTo(InterledgerAddress.of("example.destination"));
    assertThat(result.originalAmount()).isEqualTo(ZERO);
    assertThat(result.amountDelivered()).isEqualTo(UnsignedLong.ONE);
    assertThat(result.amountSent()).isEqualTo(UnsignedLong.valueOf(2));
    assertThat(result.amountLeftToSend()).isEqualTo(UnsignedLong.valueOf(3));
    assertThat(result.numFulfilledPackets()).isEqualTo(10);
    assertThat(result.numRejectPackets()).isEqualTo(5);
    assertThat(result.sendMoneyDuration()).isEqualTo(Duration.ofMillis(1000));
    assertThat(result.successfulPayment()).isTrue();
  }

  @Test
  public void testBuilderWithSenderAddress() {
    SendMoneyResult result = SendMoneyResult.builder()
        .senderAddress(InterledgerAddress.of("example.sender"))
        .destinationAddress(InterledgerAddress.of("example.destination"))
        .originalAmount(ZERO)
        .amountDelivered(UnsignedLong.ONE)
        .amountSent(UnsignedLong.valueOf(2))
        .amountLeftToSend(UnsignedLong.valueOf(3))
        .numFulfilledPackets(10)
        .numRejectPackets(5)
        .sendMoneyDuration(Duration.ofMillis(1000))
        .successfulPayment(false)
        .build();
    assertThat(result.senderAddress()).isEqualTo(InterledgerAddress.of("example.sender"));
    assertThat(result.destinationAddress()).isEqualTo(InterledgerAddress.of("example.destination"));
    assertThat(result.originalAmount()).isEqualTo(ZERO);
    assertThat(result.amountDelivered()).isEqualTo(UnsignedLong.ONE);
    assertThat(result.amountSent()).isEqualTo(UnsignedLong.valueOf(2));
    assertThat(result.amountLeftToSend()).isEqualTo(UnsignedLong.valueOf(3));
    assertThat(result.numFulfilledPackets()).isEqualTo(10);
    assertThat(result.numRejectPackets()).isEqualTo(5);
    assertThat(result.sendMoneyDuration()).isEqualTo(Duration.ofMillis(1000));
    assertThat(result.successfulPayment()).isFalse();
  }
}
