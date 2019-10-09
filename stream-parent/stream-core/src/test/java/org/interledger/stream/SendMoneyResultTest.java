package org.interledger.stream;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

import java.time.Duration;

/**
 * Unit tests for {@link SendMoneyResult}.
 */
public class SendMoneyResultTest {

  @Test
  public void totalPackets() {
    SendMoneyResult result = SendMoneyResult.builder()
        .originalAmount(UnsignedLong.ZERO)
        .amountDelivered(UnsignedLong.ZERO)
        .amountSent(UnsignedLong.ZERO)
        .amountLeftToSend(UnsignedLong.ZERO)
        .numFulfilledPackets(10)
        .numRejectPackets(5)
        .sendMoneyDuration(Duration.ZERO)
        .successfulPayment(true)
        .build();
    assertThat(result.totalPackets()).isEqualTo(15);
  }
}
