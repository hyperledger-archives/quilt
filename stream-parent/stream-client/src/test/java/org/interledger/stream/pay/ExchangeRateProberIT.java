package org.interledger.stream.pay;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.primitives.UnsignedLong;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import org.interledger.core.InterledgerAddress;
import org.interledger.fx.Denomination;
import org.interledger.link.Link;
import org.interledger.link.LinkId;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.spsp.client.SimpleSpspClient;
import org.interledger.stream.crypto.AesGcmStreamEncryptionService;
import org.interledger.stream.crypto.StreamEncryptionService;
import org.interledger.stream.crypto.StreamEncryptionUtils;
import org.interledger.stream.model.AccountDetails;
import org.interledger.stream.pay.ExchangeRateProber.Default;
import org.interledger.stream.pay.model.ExchangeRateProbeOutcome;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link ExchangeRateProber}.
 */
public class ExchangeRateProberIT extends AbstractIT {

  private ExchangeRateProber exchangeRateProber;
  private Link link;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    this.link = this.newIlpOverHttpLink(getSenderAddress());
    link.setLinkId(LinkId.of("ExchangeRateProbeIT-Link"));

    final StreamEncryptionService streamEncryptionService = new AesGcmStreamEncryptionService();
    this.exchangeRateProber = new Default(
      new StreamEncryptionUtils(streamEncryptionService),
      link
    );
  }

  @Test
  public void testRateProbeHappyPath() {
    final StreamConnection streamConnection = this.getNewStreamConnection(
      AccountDetails.builder()
        .interledgerAddress(getSenderAddress())
        .denomination(Denomination.builder().assetCode("XRP").assetScale((short) 9).build())
        .build(),
      PaymentPointer.of(RAFIKI_PAYMENT_POINTER)
    );

    final ExchangeRateProbeOutcome exchangeRateProbeOutcome = exchangeRateProber.probePath(streamConnection);

    // The current price of XRP in USD is ~$0.24. Thus, for this test we only really care if the price is accurate to
    // within a 100% margin of error.
    assertThat(exchangeRateProbeOutcome.lowerBoundRate().toBigDecimal().setScale(9, RoundingMode.HALF_UP))
      .isBetween(new BigDecimal("0.000201111"), new BigDecimal("0.000301111"));
    assertThat(exchangeRateProbeOutcome.upperBoundRate().toBigDecimal().setScale(9, RoundingMode.HALF_UP))
      .isBetween(new BigDecimal("0.000201111"), new BigDecimal("0.000301111"));

    assertThat(exchangeRateProbeOutcome.maxPacketAmount().value().isPresent()).isTrue();
    assertThat(exchangeRateProbeOutcome.maxPacketAmount().value().get()).isEqualTo(UnsignedLong.valueOf(999999999999L));
  }

  /**
   * Validates the rate probe when the max-packet size in the payment path is 50,000.
   */
  @Test
  public void testRateProbeWith50kMaxPacketLimit() {
    final StreamConnection streamConnection = this.getNewStreamConnection(
      AccountDetails.builder()
        .interledgerAddress(getSenderAddress())
        .denomination(Denomination.builder().assetCode("XRP").assetScale((short) 9).build())
        .build(),
      PaymentPointer.of(RIPPLEX_PACKET_LIMITED_PAYMENT_POINTER)
    );
    ExchangeRateProbeOutcome exchangeRateProbeOutcome = exchangeRateProber.probePath(streamConnection);

    // For this path, the FX rate is 1, so we expect the uppser bound to be 1 more than the lower bound.
    assertThat(exchangeRateProbeOutcome.lowerBoundRate().toBigDecimal().setScale(0)).isEqualTo(BigDecimal.ONE);
    assertThat(exchangeRateProbeOutcome.upperBoundRate().toBigDecimal().setScale(0)).isEqualTo(BigDecimal.ONE);

    // TODO: Fix this once https://github.com/interledger4j/ilpv4-connector/issues/660 is fixed.
//    assertThat(exchangeRateProbeOutcome.maxPacketAmount().value().isPresent()).isTrue();
//    assertThat(exchangeRateProbeOutcome.maxPacketAmount().value().get()).isEqualTo(UnsignedLong.valueOf(50000L));
  }

  //////////////////
  // Private Helpers
  //////////////////

  /**
   * Helper to get a new {@link StreamConnection} for the supplied {@link PaymentPointer}.
   *
   * @return A newly constructed and obtained {@link StreamConnection}.
   */
  private StreamConnection getNewStreamConnection(
    final AccountDetails sourceAccountDetails, final PaymentPointer paymentPointer
  ) {
    Objects.requireNonNull(sourceAccountDetails);
    Objects.requireNonNull(paymentPointer);

    final SimpleSpspClient spspClient = new SimpleSpspClient();

    // Fetch shared secret and destination address using SPSP client
    StreamConnectionDetails streamConnectionDetails = spspClient.getStreamConnectionDetails(paymentPointer);

    // TODO: Consider getting client details via IL-DCP.
    return new StreamConnection(
      sourceAccountDetails,
      streamConnectionDetails.destinationAddress(),
      streamConnectionDetails.sharedSecret()
    );
  }

  private InterledgerAddress getSenderAddress() {
    return InterledgerAddress.of(getSenderAddressPrefix().with("ExchangeRateProberIT" .toLowerCase()).getValue());
  }
}