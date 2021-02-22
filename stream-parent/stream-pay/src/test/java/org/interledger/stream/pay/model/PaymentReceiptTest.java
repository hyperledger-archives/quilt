package org.interledger.stream.pay.model;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.interledger.core.fluent.Ratio;

import org.junit.Test;

import java.math.BigInteger;
import java.time.Duration;

public class PaymentReceiptTest {

  @Test
  public void unsuccessfulPayment() {
    assertThat(PaymentReceipt.builder()
      .paymentStatistics(PaymentStatistics.builder()
        .numFulfilledPackets(1)
        .numRejectPackets(1)
        .lowerBoundExchangeRate(Ratio.ONE)
        .upperBoundExchangeRate(Ratio.ONE)
        .paymentDuration(Duration.ZERO)
        .build())
      .originalQuote(mock(Quote.class))
      .amountSentInSendersUnits(BigInteger.ONE)
      .amountDeliveredInDestinationUnits(BigInteger.ONE)
      .amountLeftToSendInSendersUnits(BigInteger.ONE)
      .build().successfulPayment())
      .isFalse();
  }

  @Test
  public void successfulPayment() {
    assertThat(PaymentReceipt.builder()
      .paymentStatistics(PaymentStatistics.builder()
        .numFulfilledPackets(1)
        .numRejectPackets(1)
        .lowerBoundExchangeRate(Ratio.ONE)
        .upperBoundExchangeRate(Ratio.ONE)
        .paymentDuration(Duration.ZERO)
        .build())
      .originalQuote(mock(Quote.class))
      .amountSentInSendersUnits(BigInteger.ONE)
      .amountDeliveredInDestinationUnits(BigInteger.ONE)
      .amountLeftToSendInSendersUnits(BigInteger.ZERO)
      .build().successfulPayment())
      .isTrue();
  }
}