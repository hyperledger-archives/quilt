package org.interledger.stream.pay.trackers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

import org.interledger.core.fluent.Ratio;
import org.interledger.stream.pay.exceptions.StreamPayerException;
import org.interledger.stream.pay.probing.model.PaymentTargetConditions;

import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Unit tests for {@link AmountTracker}.
 */
public class AmountTrackerTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private ExchangeRateTracker exchangeRateTrackerMock;

  private AmountTracker amountTracker;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    this.amountTracker = new AmountTracker(exchangeRateTrackerMock);
  }

  ////////////////////////
  // test Getters
  ////////////////////////

  @Test
  public void testGetters() {
    ExchangeRateTracker exchangeRateTracker = mock(ExchangeRateTracker.class);

    PaymentTargetConditions paymentTargetConditionsMock = mock(PaymentTargetConditions.class);
    AtomicReference paymentTargetConditionsAtomicReference = new AtomicReference(paymentTargetConditionsMock);
    AtomicReference amountSentInSourceUnitsRef = new AtomicReference(BigInteger.valueOf(2L));
    AtomicReference amountDeliveredInDestinationUnitsRef = new AtomicReference(BigInteger.valueOf(3L));
    AtomicReference sourceAmountInFlightRef = new AtomicReference(BigInteger.valueOf(4L));
    AtomicReference destinationAmountInFlightRef = new AtomicReference(BigInteger.valueOf(5L));
    AtomicReference availableDeliveryShortfallRef = new AtomicReference(BigInteger.valueOf(6L));
    AtomicReference remoteReceivedMaxRef = new AtomicReference(Optional.of(UnsignedLong.valueOf(7L)));
    AtomicBoolean encounteredProtocolViolation = new AtomicBoolean(true);

    this.amountTracker = new AmountTracker(
      exchangeRateTracker, paymentTargetConditionsAtomicReference, amountSentInSourceUnitsRef,
      amountDeliveredInDestinationUnitsRef, sourceAmountInFlightRef, destinationAmountInFlightRef,
      availableDeliveryShortfallRef, remoteReceivedMaxRef, encounteredProtocolViolation
    );

    assertThat(amountTracker.getPaymentTargetConditions().get()).isEqualTo(paymentTargetConditionsMock);
    assertThat(amountTracker.getAmountSentInSourceUnits()).isEqualTo(BigInteger.valueOf(2L));
    assertThat(amountTracker.getAmountDeliveredInDestinationUnits()).isEqualTo(BigInteger.valueOf(3L));
    assertThat(amountTracker.getSourceAmountInFlight()).isEqualTo(BigInteger.valueOf(4L));
    assertThat(amountTracker.getDestinationAmountInFlight()).isEqualTo(BigInteger.valueOf(5L));
    assertThat(amountTracker.getAvailableDeliveryShortfall()).isEqualTo(BigInteger.valueOf(6L));
    assertThat(amountTracker.getRemoteReceivedMax().get()).isEqualTo(UnsignedLong.valueOf(7L));
    assertThat(amountTracker.encounteredProtocolViolation()).isTrue();
  }

  /**
   * Using an executor with 10 threads, increment and decrement each value 10 times.
   */
  @Test
  public void testSettersInMultipleThreads() throws ExecutionException, InterruptedException {

    final ExecutorService executor = Executors.newFixedThreadPool(10);

    Runnable sourceInFlight = () -> {
      amountTracker.addToSourceAmountInFlight(UnsignedLong.ONE);
      amountTracker.subtractFromSourceAmountInFlight(UnsignedLong.ONE);
    };
    Runnable destInFlight = () -> {
      amountTracker.addToDestinationAmountInFlight(UnsignedLong.ONE);
      amountTracker.subtractFromDestinationAmountInFlight(UnsignedLong.ONE);
    };
    Runnable deliveryShortfall = () -> {
      amountTracker.increaseDeliveryShortfall(UnsignedLong.ONE);
      amountTracker.reduceDeliveryShortfall(UnsignedLong.ONE);
    };
    Runnable amountSent = () -> {
      amountTracker.addAmountSent(UnsignedLong.ONE);
    };
    Runnable amountDelivered = () -> {
      amountTracker.addAmountDelivered(UnsignedLong.ONE);
    };
    Runnable remoteMax = () -> {
      amountTracker.updateRemoteMax(UnsignedLong.ONE);
    };
    Runnable protocolViolation = () -> {
      amountTracker.setEncounteredProtocolViolation();
    };

    // Assert that Runnables are correct.
    executor.submit(sourceInFlight).get();
    executor.submit(destInFlight).get();
    executor.submit(deliveryShortfall).get();
    executor.submit(amountSent).get();
    executor.submit(amountDelivered).get();
    executor.submit(remoteMax).get();
    executor.submit(protocolViolation).get();
    assertThat(amountTracker.getSourceAmountInFlight()).isEqualTo(BigInteger.ZERO);
    assertThat(amountTracker.getDestinationAmountInFlight()).isEqualTo(BigInteger.ZERO);
    assertThat(amountTracker.getAvailableDeliveryShortfall()).isEqualTo(BigInteger.ZERO);
    assertThat(amountTracker.getAmountSentInSourceUnits()).isEqualTo(BigInteger.ONE);
    assertThat(amountTracker.getAmountDeliveredInDestinationUnits()).isEqualTo(BigInteger.ONE);
    assertThat(amountTracker.getRemoteReceivedMax().get()).isEqualTo(UnsignedLong.ONE);
    assertThat(amountTracker.encounteredProtocolViolation()).isTrue();

    final int numLoops = 10000;
    for (int i = 0; i < numLoops; i++) {
      executor.submit(sourceInFlight);
      executor.submit(destInFlight);
      executor.submit(deliveryShortfall);
      executor.submit(amountSent);
      executor.submit(amountDelivered);
      executor.submit(remoteMax);
      executor.submit(protocolViolation);
    }

    executor.shutdown();
    executor.awaitTermination(30, TimeUnit.SECONDS);

    assertThat(amountTracker.getSourceAmountInFlight()).isEqualTo(BigInteger.ZERO);
    assertThat(amountTracker.getDestinationAmountInFlight()).isEqualTo(BigInteger.ZERO);
    assertThat(amountTracker.getAvailableDeliveryShortfall()).isEqualTo(BigInteger.ZERO);
    assertThat(amountTracker.getAmountSentInSourceUnits()).isEqualTo(BigInteger.valueOf(numLoops).add(BigInteger.ONE));
    assertThat(amountTracker.getAmountDeliveredInDestinationUnits())
      .isEqualTo(BigInteger.valueOf(numLoops).add(BigInteger.ONE));
    assertThat(amountTracker.getRemoteReceivedMax().get()).isEqualTo(UnsignedLong.ONE);
    assertThat(amountTracker.encounteredProtocolViolation()).isTrue();
  }

  ////////////////////////
  // validateExchangeRates
  ////////////////////////

  @Test
  public void validateExchangeRatesWithNullLowerBound() {
    expectedException.expect(NullPointerException.class);
    amountTracker.validateExchangeRates(null, Ratio.ONE);
  }

  @Test
  public void validateExchangeRatesWithNullMinExchangeRate() {
    expectedException.expect(NullPointerException.class);
    amountTracker.validateExchangeRates(Ratio.ONE, null);
  }

  @Test
  public void validateExchangeRatesWithInvalidRate() {
    expectedException.expect(NullPointerException.class);
    amountTracker.validateExchangeRates(Ratio.ONE, null);
  }

  @Test
  public void validateExchangeRatesInsuficcientDistance() {
    expectedException.expect(StreamPayerException.class);
    amountTracker.validateExchangeRates(
      Ratio.from(new BigDecimal("1.5")),
      Ratio.from(new BigDecimal("1.1"))
    );
  }

  @Test
  public void validateExchangeRatesInsuficcientDistance2() {
    expectedException.expect(StreamPayerException.class);
    amountTracker.validateExchangeRates(
      Ratio.from(new BigDecimal("1.2")),
      Ratio.from(new BigDecimal("1.1"))
    );
  }

  @Test
  public void validateExchangeRatesInsuficcientDistance3() {
    expectedException.expect(StreamPayerException.class);
    amountTracker.validateExchangeRates(
      Ratio.from(new BigDecimal("1.0")),
      Ratio.from(new BigDecimal("1.1"))
    );
  }

  @Test
  public void validateExchangeRatesInsuficcientDistance4() {
    expectedException.expect(StreamPayerException.class);
    amountTracker.validateExchangeRates(
      Ratio.from(new BigDecimal("1.0")),
      Ratio.from(new BigDecimal("1.9"))
    );
  }

  @Test
  public void validateExchangeRatesInsuficcientDistance5() {
    expectedException.expect(StreamPayerException.class);
    amountTracker.validateExchangeRates(
      Ratio.from(new BigDecimal("1.9")),
      Ratio.from(new BigDecimal("2.0"))
    );
  }

  @Test
  public void validateExchangeRatesInsuficcientDistance6() {
    expectedException.expect(StreamPayerException.class);
    amountTracker.validateExchangeRates(
      Ratio.from(new BigDecimal("1.0")),
      Ratio.from(new BigDecimal("2.0"))
    );
  }

  @Test
  public void validateExchangeRatesInsuficcientDistance7() {
    expectedException.expect(StreamPayerException.class);
    amountTracker.validateExchangeRates(
      Ratio.from(new BigDecimal("1.9")),
      Ratio.from(new BigDecimal("1.9"))
    );
  }

  @Test
  public void validateExchangeRatesInsuficcientDistance8() {
    expectedException.expect(StreamPayerException.class);
    amountTracker.validateExchangeRates(
      Ratio.from(new BigDecimal("1.6")),
      Ratio.from(new BigDecimal("1.1"))
    );
  }

  @Test
  public void validateExchangeRatesInsuficcientDistance9() {
    expectedException.expect(StreamPayerException.class);
    amountTracker.validateExchangeRates(
      Ratio.from(BigDecimal.ZERO),
      Ratio.from(BigDecimal.ONE)
    );
  }

  @Test
  public void testValidateExchangeRates() {
    // ExpectedException is none...

    amountTracker.validateExchangeRates(Ratio.ZERO, Ratio.ZERO);
    amountTracker.validateExchangeRates(Ratio.ONE, Ratio.ONE);
    amountTracker.validateExchangeRates(Ratio.ONE, Ratio.ZERO);

    amountTracker.validateExchangeRates(
      Ratio.from(new BigDecimal("20.0")),
      Ratio.from(new BigDecimal("1.9"))
    );
    amountTracker.validateExchangeRates(
      Ratio.from(new BigDecimal("2.0")),
      Ratio.from(new BigDecimal("1.9"))
    );
    amountTracker.validateExchangeRates(
      Ratio.from(new BigDecimal("2.1")),
      Ratio.from(new BigDecimal("2.0"))
    );
    amountTracker.validateExchangeRates(
      Ratio.from(new BigDecimal("2.1")),
      Ratio.from(new BigDecimal("1.9"))
    );
    amountTracker.validateExchangeRates(
      Ratio.from(new BigDecimal("2.1")),
      Ratio.from(new BigDecimal("1.1"))
    );
    amountTracker.validateExchangeRates(
      Ratio.from(new BigDecimal("2.1")),
      Ratio.from(new BigDecimal("1.0"))
    );
    amountTracker.validateExchangeRates(
      Ratio.from(new BigDecimal("2.1")),
      Ratio.from(new BigDecimal("0.9"))
    );
    amountTracker.validateExchangeRates(
      Ratio.from(BigDecimal.ONE),
      Ratio.from(BigDecimal.ZERO)
    );
  }

  //////////////////////////
  // validateMinPacketAmount
  //////////////////////////

  @Test
  public void testValidateMinPacketAmountWithNullLowerBound() {
    expectedException.expect(NullPointerException.class);
    amountTracker.validateMinPacketAmount(null, Ratio.ONE, UnsignedLong.ONE);
  }

  @Test
  public void testValidateMinPacketAmountWithNullMinFxRate() {
    expectedException.expect(NullPointerException.class);
    amountTracker.validateMinPacketAmount(Ratio.ONE, null, UnsignedLong.ONE);
  }

  @Test
  public void testValidateMinPacketAmountWithNullMaxPacketAmount() {
    expectedException.expect(NullPointerException.class);
    amountTracker.validateMinPacketAmount(Ratio.ONE, Ratio.ONE, null);
  }

  @Test
  public void testValidateMinPacketAmountWithLargeMaxPacket1() {
    expectedException.expect(StreamPayerException.class);
    amountTracker.validateMinPacketAmount(
      Ratio.from(new BigDecimal("1.5")),
      Ratio.from(new BigDecimal("1.1")),
      UnsignedLong.ONE
    );
  }


  @Test
  public void testValidateMinPacketAmountWithLargeMaxPacket2() {
    expectedException.expect(StreamPayerException.class);
    amountTracker.validateMinPacketAmount(
      Ratio.from(new BigDecimal("1.2")),
      Ratio.from(new BigDecimal("1.1")),
      UnsignedLong.ONE
    );
  }

  @Test
  public void testValidateMinPacketAmountWithLargeMaxPacket3() {
    expectedException.expect(StreamPayerException.class);
    amountTracker.validateMinPacketAmount(
      Ratio.from(new BigDecimal("1.6")),
      Ratio.from(new BigDecimal("1.1")),
      UnsignedLong.ONE
    );
  }

  @Test
  public void testValidateMinPacketAmountWithLargeMaxPacket4() {
    expectedException.expect(StreamPayerException.class);
    amountTracker.validateMinPacketAmount(
      Ratio.from(new BigDecimal("2.1")),
      Ratio.from(new BigDecimal("2.0")),
      UnsignedLong.ONE
    );
  }

  @Test
  public void testValidateMinPacketAmountWithSmallMaxPacket1() {
    expectedException.expect(StreamPayerException.class);
    amountTracker.validateMinPacketAmount(
      Ratio.from(new BigDecimal("2.0")),
      Ratio.from(new BigDecimal("1.9")),
      UnsignedLong.ONE
    );
  }

  @Test
  public void testValidateMinPacketAmountWithSmallMaxPacket2() {
    expectedException.expect(StreamPayerException.class);
    amountTracker.validateMinPacketAmount(
      Ratio.from(new BigDecimal("2.1")),
      Ratio.from(new BigDecimal("2.0")),
      UnsignedLong.ONE
    );
  }

  @Test
  public void testValidateMinPacketAmountWithSmallMaxPacket3() {
    expectedException.expect(StreamPayerException.class);
    amountTracker.validateMinPacketAmount(
      Ratio.from(new BigDecimal("2.1")),
      Ratio.from(new BigDecimal("1.9")),
      UnsignedLong.ONE
    );
  }

  @Test
  public void testValidateMinPacketAmountWithLargeMax() {
    // ExpectedException is none...

    amountTracker.validateMinPacketAmount(Ratio.ZERO, Ratio.ZERO, UnsignedLong.MAX_VALUE);
    amountTracker.validateMinPacketAmount(Ratio.ONE, Ratio.ONE, UnsignedLong.MAX_VALUE);
    amountTracker.validateMinPacketAmount(Ratio.ONE, Ratio.ZERO, UnsignedLong.MAX_VALUE);

    amountTracker.validateMinPacketAmount(
      Ratio.from(new BigDecimal("1.0")),
      Ratio.from(new BigDecimal("0.0")),
      UnsignedLong.MAX_VALUE
    );
    amountTracker.validateMinPacketAmount(
      Ratio.from(new BigDecimal("1.0")),
      Ratio.from(new BigDecimal("1.1")),
      UnsignedLong.MAX_VALUE
    );
    amountTracker.validateMinPacketAmount(
      Ratio.from(new BigDecimal("1.0")),
      Ratio.from(new BigDecimal("1.9")),
      UnsignedLong.MAX_VALUE
    );
    amountTracker.validateMinPacketAmount(
      Ratio.from(new BigDecimal("1.0")),
      Ratio.from(new BigDecimal("2.0")),
      UnsignedLong.MAX_VALUE
    );
    amountTracker.validateMinPacketAmount(
      Ratio.from(new BigDecimal("1.9")),
      Ratio.from(new BigDecimal("1.9")),
      UnsignedLong.MAX_VALUE
    );
    amountTracker.validateMinPacketAmount(
      Ratio.from(new BigDecimal("1.9")),
      Ratio.from(new BigDecimal("2.0")),
      UnsignedLong.MAX_VALUE
    );
    amountTracker.validateMinPacketAmount(
      Ratio.from(new BigDecimal("2.0")),
      Ratio.from(new BigDecimal("1.9")),
      UnsignedLong.MAX_VALUE
    );
    amountTracker.validateMinPacketAmount(
      Ratio.from(new BigDecimal("2.1")),
      Ratio.from(new BigDecimal("2.0")),
      UnsignedLong.MAX_VALUE
    );
    amountTracker.validateMinPacketAmount(
      Ratio.from(new BigDecimal("2.1")),
      Ratio.from(new BigDecimal("1.9")),
      UnsignedLong.MAX_VALUE
    );
    amountTracker.validateMinPacketAmount(
      Ratio.from(new BigDecimal("2.1")),
      Ratio.from(new BigDecimal("1.1")),
      UnsignedLong.MAX_VALUE
    );
    amountTracker.validateMinPacketAmount(
      Ratio.from(new BigDecimal("2.1")),
      Ratio.from(new BigDecimal("1.0")),
      UnsignedLong.MAX_VALUE
    );
    amountTracker.validateMinPacketAmount(
      Ratio.from(new BigDecimal("2.1")),
      Ratio.from(new BigDecimal("0.9")),
      UnsignedLong.MAX_VALUE
    );
    amountTracker.validateMinPacketAmount(
      Ratio.from(new BigDecimal("20.0")),
      Ratio.from(new BigDecimal("1.9")),
      UnsignedLong.MAX_VALUE
    );
  }

  @Test
  public void testValidateMinPacketAmountWithSmallMax() {
    // ExpectedException is none...

    amountTracker.validateMinPacketAmount(Ratio.ZERO, Ratio.ZERO, UnsignedLong.ONE);
    amountTracker.validateMinPacketAmount(Ratio.ONE, Ratio.ONE, UnsignedLong.ONE);
    amountTracker.validateMinPacketAmount(Ratio.ONE, Ratio.ZERO, UnsignedLong.ONE);

    amountTracker.validateMinPacketAmount(
      Ratio.from(new BigDecimal("1.0")),
      Ratio.from(new BigDecimal("0.0")),
      UnsignedLong.ONE
    );
    amountTracker.validateMinPacketAmount(
      Ratio.from(new BigDecimal("1.0")),
      Ratio.from(new BigDecimal("1.1")),
      UnsignedLong.ONE
    );
    amountTracker.validateMinPacketAmount(
      Ratio.from(new BigDecimal("1.0")),
      Ratio.from(new BigDecimal("1.9")),
      UnsignedLong.ONE
    );
    amountTracker.validateMinPacketAmount(
      Ratio.from(new BigDecimal("1.0")),
      Ratio.from(new BigDecimal("2.0")),
      UnsignedLong.ONE
    );
    amountTracker.validateMinPacketAmount(
      Ratio.from(new BigDecimal("1.9")),
      Ratio.from(new BigDecimal("1.9")),
      UnsignedLong.ONE
    );
    amountTracker.validateMinPacketAmount(
      Ratio.from(new BigDecimal("1.9")),
      Ratio.from(new BigDecimal("2.0")),
      UnsignedLong.ONE
    );
    amountTracker.validateMinPacketAmount(
      Ratio.from(new BigDecimal("2.1")),
      Ratio.from(new BigDecimal("1.1")),
      UnsignedLong.ONE
    );
    amountTracker.validateMinPacketAmount(
      Ratio.from(new BigDecimal("2.1")),
      Ratio.from(new BigDecimal("1.0")),
      UnsignedLong.ONE
    );
    amountTracker.validateMinPacketAmount(
      Ratio.from(new BigDecimal("2.1")),
      Ratio.from(new BigDecimal("0.9")),
      UnsignedLong.ONE
    );
    amountTracker.validateMinPacketAmount(
      Ratio.from(new BigDecimal("20.0")),
      Ratio.from(new BigDecimal("1.9")),
      UnsignedLong.ONE
    );
  }
}