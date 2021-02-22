package org.interledger.stream.pay.model;


import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.Test;

/**
 * Unit test for {@link SendState}.
 */
public class SendStateTest {

  @Test
  public void isPaymentError() {
    assertThat(SendState.Ready.isPaymentError()).isFalse();
    assertThat(SendState.Wait.isPaymentError()).isFalse();
    assertThat(SendState.End.isPaymentError()).isFalse();
    assertThat(SendState.InvalidPaymentPointer.isPaymentError()).isTrue();
    assertThat(SendState.InvalidCredentials.isPaymentError()).isTrue();
    assertThat(SendState.Disconnected.isPaymentError()).isTrue();
    assertThat(SendState.InvalidSlippage.isPaymentError()).isTrue();
    assertThat(SendState.IncompatibleInterledgerNetworks.isPaymentError()).isTrue();
    assertThat(SendState.UnknownSourceAsset.isPaymentError()).isTrue();
    assertThat(SendState.UnknownPaymentTarget.isPaymentError()).isTrue();
    assertThat(SendState.InvalidSourceAmount.isPaymentError()).isTrue();
    assertThat(SendState.InvalidDestinationAmount.isPaymentError()).isTrue();
    assertThat(SendState.UnenforceableDelivery.isPaymentError()).isTrue();
    assertThat(SendState.InvoiceAlreadyPaid.isPaymentError()).isTrue();
    assertThat(SendState.ExternalRateUnavailable.isPaymentError()).isTrue();
    assertThat(SendState.InsufficientExchangeRate.isPaymentError()).isTrue();
    assertThat(SendState.UnknownDestinationAsset.isPaymentError()).isTrue();
    assertThat(SendState.DestinationAssetConflict.isPaymentError()).isTrue();
    assertThat(SendState.IncompatibleReceiveMax.isPaymentError()).isTrue();
    assertThat(SendState.ClosedByRecipient.isPaymentError()).isTrue();
    assertThat(SendState.ReceiverProtocolViolation.isPaymentError()).isTrue();
    assertThat(SendState.RateProbeFailed.isPaymentError()).isTrue();
    assertThat(SendState.IdleTimeout.isPaymentError()).isTrue();
    assertThat(SendState.ConnectorError.isPaymentError()).isTrue();
    assertThat(SendState.ExceededMaxSequence.isPaymentError()).isTrue();
    assertThat(SendState.ExchangeRateRoundingError.isPaymentError()).isTrue();
  }
}