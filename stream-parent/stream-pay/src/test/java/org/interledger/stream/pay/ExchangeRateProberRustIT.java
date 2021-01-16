package org.interledger.stream.pay;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.core.fluent.Ratio;
import org.interledger.fx.Denominations;
import org.interledger.link.Link;
import org.interledger.spsp.PaymentPointer;
import org.interledger.stream.connection.StreamConnection;
import org.interledger.stream.model.AccountDetails;
import org.interledger.stream.pay.probing.ExchangeRateProber;
import org.interledger.stream.pay.probing.model.ExchangeRateProbeOutcome;
import org.interledger.stream.pay.trackers.MaxPacketAmountTracker.MaxPacketState;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

import java.math.BigInteger;

/**
 * Unit tests for {@link org.interledger.stream.pay.probing.ExchangeRateProber}.
 */
// TODO [NewFeature]: Create an IT that hits the Java Connector via TestContainers.
@SuppressWarnings("OptionalGetWithoutIsPresent")
public class ExchangeRateProberRustIT extends AbstractRustIT {

  /**
   * Tests the rate probe on a path that has a max-packet value of 50k and an FX using an XRP to USD.
   */
  @Test
  public void testRateProbeWithMaxPacket50kViaXrpToUsd() {
    final Link<?> ilpLink = this.constructIlpOverHttpLink(XRP_ACCOUNT_50K); // <-- All ILP operations from XRP_ACCOUNT
    final AccountDetails senderAccountDetails = newSenderAccountDetailsViaILDCP(ilpLink);
    ExchangeRateProber.Default exchangeRateProber = new ExchangeRateProber.Default(streamPacketEncryptionService, ilpLink);

    final StreamConnection streamConnection = this.getNewStreamConnection(
      senderAccountDetails, PaymentPointer.of(PAYMENT_POINTER_USD_50K)
    );

    final ExchangeRateProbeOutcome exchangeRateProbeOutcome = exchangeRateProber.probePath(streamConnection);

    assertThat(exchangeRateProbeOutcome.sourceDenomination().get()).isEqualTo(Denominations.XRP_MILLI_DROPS);
    assertThat(exchangeRateProbeOutcome.destinationDenomination().get()).isEqualTo(Denominations.USD_MILLI_DOLLARS);
    assertThat(exchangeRateProbeOutcome.lowerBoundRate()).isEqualTo(
      Ratio.builder().numerator(BigInteger.valueOf(2)).denominator(BigInteger.valueOf(10000L)).build()
    );
    assertThat(exchangeRateProbeOutcome.upperBoundRate()).isEqualTo(
      Ratio.builder().numerator(BigInteger.valueOf(3)).denominator(BigInteger.valueOf(10000L)).build()
    );

    // Even though none of the probe packets are exactly 50k, The MaxPacketAmount should be discovered imprecisely.
    // This is especially true for the Java Connector, which has a bug that doesn't send meta-data.
    // See https://github.com/interledger4j/ilpv4-connector/issues/660
    // assertThat(exchangeRateProbeOutcome.maxPacketAmount().maxPacketState()).isEqualTo(MaxPacketState.ImpreciseMax);
    // assertThat(exchangeRateProbeOutcome.maxPacketAmount().value()).isEqualTo(UnsignedLong.valueOf(99999L));
    // assertThat(exchangeRateProbeOutcome.verifiedPathCapacity()).isEqualTo(UnsignedLong.valueOf(10000L));

    // Likewise, for the RustConnector, it doesn't send _any_ F08 rejections for the max-packet amount.
    assertThat(exchangeRateProbeOutcome.maxPacketAmount().maxPacketState()).isEqualTo(MaxPacketState.PreciseMax);
    assertThat(exchangeRateProbeOutcome.maxPacketAmount().value()).isEqualTo(UnsignedLong.valueOf(50000L));
    // This value is 10000 because the probe amount of 100000 is rejected via F08, so we never actually validate
    // anything greater than 10,000.
    assertThat(exchangeRateProbeOutcome.verifiedPathCapacity()).isEqualTo(UnsignedLong.valueOf(10000L));
  }

  /**
   * Tests the rate probe on a path that has an unlimited max-packet value and a 1:1 FX using an XRP to XRP payment.
   * <pre>
   * Sender Account: `demo_user`
   * Receiver Account: $rafiki.money/p/test@example.com
   * </pre>
   */
  @Test
  public void testRateProbeUnlimitedMaxPathWithXrpToXrp() {
    final Link<?> ilpLink = this.constructIlpOverHttpLink(XRP_ACCOUNT); // <-- All ILP operations from XRP_ACCOUNT
    final AccountDetails senderAccountDetails = newSenderAccountDetailsViaILDCP(ilpLink);
    ExchangeRateProber.Default exchangeRateProber = new ExchangeRateProber.Default(streamPacketEncryptionService, ilpLink);

    final StreamConnection streamConnection = this.getNewStreamConnection(
      senderAccountDetails, PaymentPointer.of(PAYMENT_POINTER_XRP)
    );

    final ExchangeRateProbeOutcome exchangeRateProbeOutcome = exchangeRateProber.probePath(streamConnection);

    assertThat(exchangeRateProbeOutcome.sourceDenomination().get()).isEqualTo(Denominations.XRP_MILLI_DROPS);
    assertThat(exchangeRateProbeOutcome.destinationDenomination().get()).isEqualTo(Denominations.XRP_MILLI_DROPS);
    assertThat(exchangeRateProbeOutcome.maxPacketAmount().maxPacketState()).isEqualTo(MaxPacketState.UnknownMax);
    assertThat(exchangeRateProbeOutcome.maxPacketAmount().value()).isEqualTo(UnsignedLong.MAX_VALUE);
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

}