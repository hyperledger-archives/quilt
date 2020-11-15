package org.interledger.stream;

import static com.google.common.primitives.UnsignedLong.ZERO;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.primitives.UnsignedLong;
import java.time.Duration;
import org.interledger.core.InterledgerAddress;
import org.interledger.fx.Denomination;
import org.junit.Test;

/**
 * Unit tests for {@link SendMoneyResult}.
 */
public class SendMoneyResultTest {

  private static final Denomination SENDER_DENOMINATION = Denomination.builder()
    .assetCode("USD")
    .assetScale((short) 2)
    .build();

  private static final Denomination DESTINATION_DENOMINATION = Denomination.builder()
    .assetCode("XRP")
    .assetScale((short) 9)
    .build();

  @Test
  public void testBuilderWithNoOptionals() {
    SendMoneyResult result = SendMoneyResult.builder()
      .senderDenomination(SENDER_DENOMINATION)
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
    assertThat(result.destinationDenomination()).isEmpty();
    assertThat(result.originalAmount()).isEqualTo(ZERO);
    assertThat(result.amountDelivered()).isEqualTo(UnsignedLong.ONE);
    assertThat(result.amountSent()).isEqualTo(UnsignedLong.valueOf(2));
    assertThat(result.amountLeftToSend()).isEqualTo(UnsignedLong.valueOf(3));
    assertThat(result.numFulfilledPackets()).isEqualTo(10);
    assertThat(result.numRejectPackets()).isEqualTo(5);
    assertThat(result.totalPackets()).isEqualTo(15);
    assertThat(result.sendMoneyDuration()).isEqualTo(Duration.ofMillis(1000));
    assertThat(result.successfulPayment()).isTrue();
  }

  @Test
  public void testBuilderWithOptionals() {
    SendMoneyResult result = SendMoneyResult.builder()
      .senderAddress(InterledgerAddress.of("example.sender"))
      .senderDenomination(SENDER_DENOMINATION)
      .destinationAddress(InterledgerAddress.of("example.destination"))
      .destinationDenomination(DESTINATION_DENOMINATION)
      .originalAmount(ZERO)
      .amountDelivered(UnsignedLong.ONE)
      .amountSent(UnsignedLong.valueOf(2))
      .amountLeftToSend(UnsignedLong.valueOf(3))
      .numFulfilledPackets(10)
      .numRejectPackets(5)
      .sendMoneyDuration(Duration.ofMillis(1000))
      .successfulPayment(false)
      .build();

    assertThat(result.senderAddress().get()).isEqualTo(InterledgerAddress.of("example.sender"));
    assertThat(result.senderDenomination()).isEqualTo(SENDER_DENOMINATION);
    assertThat(result.destinationAddress()).isEqualTo(InterledgerAddress.of("example.destination"));
    assertThat(result.destinationDenomination().get()).isEqualTo(DESTINATION_DENOMINATION);
    assertThat(result.originalAmount()).isEqualTo(ZERO);
    assertThat(result.amountDelivered()).isEqualTo(UnsignedLong.ONE);
    assertThat(result.amountSent()).isEqualTo(UnsignedLong.valueOf(2));
    assertThat(result.amountLeftToSend()).isEqualTo(UnsignedLong.valueOf(3));
    assertThat(result.numFulfilledPackets()).isEqualTo(10);
    assertThat(result.numRejectPackets()).isEqualTo(5);
    assertThat(result.totalPackets()).isEqualTo(15);
    assertThat(result.sendMoneyDuration()).isEqualTo(Duration.ofMillis(1000));
    assertThat(result.successfulPayment()).isFalse();
  }
}
