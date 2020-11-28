package org.interledger.stream.pay;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.primitives.UnsignedLong;
import java.math.BigInteger;
import java.util.Objects;
import org.interledger.codecs.stream.StreamCodecContextFactory;
import org.interledger.core.fluent.Ratio;
import org.interledger.fx.Denomination;
import org.interledger.fx.Denominations;
import org.interledger.link.Link;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.spsp.client.SimpleSpspClient;
import org.interledger.stream.crypto.AesGcmStreamEncryptionService;
import org.interledger.stream.crypto.StreamEncryptionUtils;
import org.interledger.stream.model.AccountDetails;
import org.interledger.stream.pay.probing.ExchangeRateProber.Default;
import org.interledger.stream.pay.probing.model.ExchangeRateProbeOutcome;
import org.interledger.stream.pay.trackers.MaxPacketAmountTracker.MaxPacketState;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link org.interledger.stream.pay.probing.ExchangeRateProber}.
 */
public class ExchangeRateProberIT extends AbstractIT {

  private StreamEncryptionUtils streamEncryptionUtils;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    this.streamEncryptionUtils = new StreamEncryptionUtils(
      StreamCodecContextFactory.oer(), new AesGcmStreamEncryptionService()
    );
  }

  /**
   * Tests the rate probe on a path that has a max-packet value of 50k and a 1:1 FX using an XRP to XRP payment.
   */
  @Test
  public void testRateProbeWithMaxPacket50kViaXrpToXrp() {
    Link link = this.newIlpOverHttpLinkForQuiltIT();
    Default exchangeRateProber = new Default(streamEncryptionUtils, link);

    final StreamConnection streamConnection = this.getNewStreamConnection(
      AccountDetails.builder()
        .interledgerAddress(getSenderAddress())
        .denomination(Denomination.builder().assetCode("XRP").assetScale((short) 9).build())
        .build(),
      PaymentPointer.of(RIPPLEX_UNLIMITED_PAYMENT_POINTER2)
    );

    final ExchangeRateProbeOutcome exchangeRateProbeOutcome = exchangeRateProber.probePath(streamConnection);

    assertThat(exchangeRateProbeOutcome.sourceDenomination().get()).isEqualTo(Denominations.XRP_MILLI_DROPS);
    assertThat(exchangeRateProbeOutcome.destinationDenomination().get()).isEqualTo(Denominations.XRP_MILLI_DROPS);
    assertThat(exchangeRateProbeOutcome.lowerBoundRate()).isEqualTo(
      Ratio.builder().numerator(BigInteger.valueOf(10000)).denominator(BigInteger.valueOf(10000)).build()
    );
    assertThat(exchangeRateProbeOutcome.upperBoundRate()).isEqualTo(
      Ratio.builder().numerator(BigInteger.valueOf(10001)).denominator(BigInteger.valueOf(10000)).build()
    );
    assertThat(exchangeRateProbeOutcome.maxPacketAmount().maxPacketState()).isEqualTo(MaxPacketState.ImpreciseMax);
    assertThat(exchangeRateProbeOutcome.maxPacketAmount().value().get()).isEqualTo(UnsignedLong.valueOf(99999L));
    assertThat(exchangeRateProbeOutcome.verifiedPathCapacity()).isEqualTo(UnsignedLong.valueOf(10000L));

  }

  /**
   * Tests the rate probe on a path that has a max-packet value of 50k and an FX using an XRP to USD.
   */
  @Test
  public void testRateProbeWithMaxPacket50kViaXrpToUsd() {
    Link link = this.newIlpOverHttpLinkForQuiltIT();
    Default exchangeRateProber = new Default(streamEncryptionUtils, link);

    final StreamConnection streamConnection = this.getNewStreamConnection(
      AccountDetails.builder()
        .interledgerAddress(getSenderAddress())
        .denomination(Denomination.builder().assetCode("XRP").assetScale((short) 9).build())
        .build(),
      PaymentPointer.of(RAFIKI_PAYMENT_POINTER)
    );

    final ExchangeRateProbeOutcome exchangeRateProbeOutcome = exchangeRateProber.probePath(streamConnection);

    assertThat(exchangeRateProbeOutcome.sourceDenomination().get()).isEqualTo(Denominations.XRP_MILLI_DROPS);
    assertThat(exchangeRateProbeOutcome.destinationDenomination().get()).isEqualTo(Denominations.USD_MILLI_DOLLARS);
    assertThat(exchangeRateProbeOutcome.lowerBoundRate()).isEqualTo(
      Ratio.builder().numerator(BigInteger.valueOf(2)).denominator(BigInteger.valueOf(10000)).build()
    );
    assertThat(exchangeRateProbeOutcome.upperBoundRate()).isEqualTo(
      Ratio.builder().numerator(BigInteger.valueOf(3)).denominator(BigInteger.valueOf(10000)).build()
    );

    // Even though none of the probe packets are exactly 50k, The MaxPacketAmount should be discovered precisely.
    // This is especially true for the Java Connector, which has a bug that doesn't send meta-data.
    // See https://github.com/interledger4j/ilpv4-connector/issues/660
    assertThat(exchangeRateProbeOutcome.maxPacketAmount().maxPacketState()).isEqualTo(MaxPacketState.ImpreciseMax);
    assertThat(exchangeRateProbeOutcome.maxPacketAmount().value().get()).isEqualTo(UnsignedLong.valueOf(99999L));
    assertThat(exchangeRateProbeOutcome.verifiedPathCapacity()).isEqualTo(UnsignedLong.valueOf(10000L));
  }

  /**
   * Tests the rate probe on a path that has an unlimited max-packet value and an FX using an XRP to USD.
   */
  @Test
  public void testRateProbeWithUnlimitedMaxViaXrpToUsd() {
    Link link = this.newIlpOverHttpLinkForDemoUser();
    Default exchangeRateProber = new Default(streamEncryptionUtils, link);

    final StreamConnection streamConnection = this.getNewStreamConnection(
      AccountDetails.builder()
        .interledgerAddress(getSenderAddress())
        .denomination(Denomination.builder().assetCode("XRP").assetScale((short) 9).build())
        .build(),
      PaymentPointer.of(RAFIKI_PAYMENT_POINTER)
    );

    final ExchangeRateProbeOutcome exchangeRateProbeOutcome = exchangeRateProber.probePath(streamConnection);

    assertThat(exchangeRateProbeOutcome.sourceDenomination().get()).isEqualTo(Denominations.XRP_MILLI_DROPS);
    assertThat(exchangeRateProbeOutcome.destinationDenomination().get()).isEqualTo(Denominations.USD_MILLI_DOLLARS);
    assertThat(exchangeRateProbeOutcome.lowerBoundRate()).isEqualTo(Ratio.builder()
      .numerator(BigInteger.valueOf(24295460)).denominator(BigInteger.valueOf(100000000000L))
      .build()
    );
    assertThat(exchangeRateProbeOutcome.upperBoundRate()).isEqualTo(Ratio.builder()
      .numerator(BigInteger.valueOf(24295461)).denominator(BigInteger.valueOf(100000000000L))
      .build()
    );
    assertThat(exchangeRateProbeOutcome.maxPacketAmount().maxPacketState()).isEqualTo(MaxPacketState.ImpreciseMax);
    assertThat(exchangeRateProbeOutcome.maxPacketAmount().value().get()).isEqualTo(UnsignedLong.valueOf(999999999999L));
    assertThat(exchangeRateProbeOutcome.verifiedPathCapacity()).isEqualTo(UnsignedLong.valueOf(100000000000L));
  }

  /**
   * Tests the rate probe on a path that has an unlimited max-packet value and a 1:1 FX using an XRP to XRP payment.
   * <p>
   * <pre>
   * Sender Account: `demo_user`
   * Receiver Account: $rafiki.money/p/test@example.com
   * </pre>
   */
  @Test
  public void testRateProbeUnlimitedMaxPathWithXrpToXrp() {
    Link link = this.newIlpOverHttpLinkForDemoUser();
    Default exchangeRateProber = new Default(streamEncryptionUtils, link);

    final StreamConnection streamConnection = this.getNewStreamConnection(
      AccountDetails.builder()
        .interledgerAddress(getSenderAddress())
        .denomination(Denomination.builder().assetCode("XRP").assetScale((short) 9).build())
        .build(),
      PaymentPointer.of(RIPPLEX_UNLIMITED_PAYMENT_POINTER2)
    );

    final ExchangeRateProbeOutcome exchangeRateProbeOutcome = exchangeRateProber.probePath(streamConnection);

    assertThat(exchangeRateProbeOutcome.sourceDenomination().get()).isEqualTo(Denominations.XRP_MILLI_DROPS);
    assertThat(exchangeRateProbeOutcome.destinationDenomination().get()).isEqualTo(Denominations.XRP_MILLI_DROPS);
    assertThat(exchangeRateProbeOutcome.maxPacketAmount().maxPacketState()).isEqualTo(MaxPacketState.UnknownMax);
    assertThat(exchangeRateProbeOutcome.maxPacketAmount().value().isPresent()).isFalse();
    assertThat(exchangeRateProbeOutcome.verifiedPathCapacity()).isEqualTo(UnsignedLong.valueOf(1000000000000L));
    assertThat(exchangeRateProbeOutcome.lowerBoundRate()).isEqualTo(Ratio.builder()
      .numerator(BigInteger.valueOf(1000000000000L))
      .denominator(BigInteger.valueOf(1000000000000L))
      .build()
    );
    assertThat(exchangeRateProbeOutcome.upperBoundRate()).isEqualTo(Ratio.builder()
      .numerator(BigInteger.valueOf(1000000000001L))
      .denominator(BigInteger.valueOf(1000000000000L))
      .build()
    );
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

}