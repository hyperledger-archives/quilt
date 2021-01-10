package org.interledger.stream.pay.trackers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.interledger.stream.pay.SendStateMatcher.hasSendState;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.interledger.core.fluent.Ratio;
import org.interledger.stream.pay.exceptions.StreamPayerException;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.probing.model.EstimatedPaymentOutcome;
import org.interledger.stream.pay.probing.model.PaymentTargetConditions;
import org.interledger.stream.pay.probing.model.PaymentTargetConditions.PaymentType;

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
@SuppressWarnings("ALL")
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
    AtomicReference availableDeliveryShortfallRef = new AtomicReference(UnsignedLong.valueOf(6L));
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
    assertThat(amountTracker.getAvailableDeliveryShortfall()).isEqualTo(UnsignedLong.valueOf(6L));
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
    assertThat(amountTracker.getAvailableDeliveryShortfall()).isEqualTo(UnsignedLong.ZERO);
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
    assertThat(amountTracker.getAvailableDeliveryShortfall()).isEqualTo(UnsignedLong.ZERO);
    assertThat(amountTracker.getAmountSentInSourceUnits()).isEqualTo(BigInteger.valueOf(numLoops).add(BigInteger.ONE));
    assertThat(amountTracker.getAmountDeliveredInDestinationUnits())
      .isEqualTo(BigInteger.valueOf(numLoops).add(BigInteger.ONE));
    assertThat(amountTracker.getRemoteReceivedMax().get()).isEqualTo(UnsignedLong.ONE);
    assertThat(amountTracker.encounteredProtocolViolation()).isTrue();
  }

  ////////////////////////
  // setPaymentTarget
  ////////////////////////

  @Test
  public void setPaymentTargetWhenNull1() {
    expectedException.expect(NullPointerException.class);
    amountTracker.setPaymentTarget(null, Ratio.ONE, UnsignedLong.ONE, BigInteger.ONE);
  }

  @Test
  public void setPaymentTargetWhenNull2() {
    expectedException.expect(NullPointerException.class);
    amountTracker.setPaymentTarget(PaymentType.FIXED_SEND, null, UnsignedLong.ONE, BigInteger.ONE);
  }

  @Test
  public void setPaymentTargetWhenNull3() {
    expectedException.expect(NullPointerException.class);
    amountTracker.setPaymentTarget(PaymentType.FIXED_SEND, Ratio.ONE, null, BigInteger.ONE);
  }

  @Test
  public void setPaymentTargetWhenNull4() {
    expectedException.expect(NullPointerException.class);
    amountTracker.setPaymentTarget(PaymentType.FIXED_SEND, Ratio.ONE, null, BigInteger.ONE);
  }

  @Test
  public void setPaymentTargetLowerBoundRateNotPositive() {
    when(exchangeRateTrackerMock.getLowerBoundRate()).thenReturn(Ratio.ZERO);
    when(exchangeRateTrackerMock.getUpperBoundRate()).thenReturn(Ratio.ONE);

    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.InsufficientExchangeRate));
    expectedException.expectMessage(containsString("Rate Probe discovered invalid exchange rates."));
    amountTracker.setPaymentTarget(PaymentType.FIXED_SEND, Ratio.ONE, UnsignedLong.ONE, BigInteger.ONE);
  }

  @Test
  public void setPaymentTargetUpperBoundRateNotPositive() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.InsufficientExchangeRate));
    expectedException.expectMessage(containsString("Rate Probe discovered invalid exchange rates."));

    when(exchangeRateTrackerMock.getLowerBoundRate()).thenReturn(Ratio.ONE);
    when(exchangeRateTrackerMock.getUpperBoundRate()).thenReturn(Ratio.ZERO);

    amountTracker.setPaymentTarget(PaymentType.FIXED_SEND, Ratio.ONE, UnsignedLong.ONE, BigInteger.ONE);
  }

  @Test
  public void setPaymentTargetWithFixedSend() {
    when(exchangeRateTrackerMock.getLowerBoundRate()).thenReturn(Ratio.ONE);
    when(exchangeRateTrackerMock.getUpperBoundRate()).thenReturn(Ratio.ONE);

    final EstimatedPaymentOutcome actual = amountTracker
      .setPaymentTarget(PaymentType.FIXED_SEND, Ratio.ONE, UnsignedLong.ONE, BigInteger.ONE);

    assertThat(amountTracker.getAvailableDeliveryShortfall()).isEqualTo(UnsignedLong.ONE);
    assertThat(amountTracker.getSourceAmountInFlight()).isEqualTo(BigInteger.ZERO);
    assertThat(amountTracker.getDestinationAmountInFlight()).isEqualTo(BigInteger.ZERO);
    assertThat(amountTracker.getAmountSentInSourceUnits()).isEqualTo(BigInteger.ZERO);
    assertThat(amountTracker.getAmountDeliveredInDestinationUnits()).isEqualTo(BigInteger.ZERO);
    assertThat(amountTracker.getRemoteReceivedMax()).isEmpty();
    assertThat(amountTracker.getPaymentTargetConditions().get().paymentType()).isEqualTo(PaymentType.FIXED_SEND);
    assertThat(amountTracker.getPaymentTargetConditions().get().maxPaymentAmountInSenderUnits())
      .isEqualTo(BigInteger.ONE);
    assertThat(amountTracker.getPaymentTargetConditions().get().minPaymentAmountInDestinationUnits())
      .isEqualTo(BigInteger.ZERO);
    assertThat(amountTracker.getPaymentTargetConditions().get().minExchangeRate().toBigDecimal())
      .isEqualTo(BigDecimal.ONE);

    assertThat(actual.estimatedNumberOfPackets()).isEqualTo(BigInteger.ONE);
    assertThat(actual.maxSendAmountInWholeSourceUnits()).isEqualTo(BigInteger.ONE);
    assertThat(actual.maxSendAmountInWholeSourceUnits()).isEqualTo(BigInteger.ONE);
  }

  @Test
  public void setPaymentTargetWithFixedDelivery() {
    when(exchangeRateTrackerMock.getLowerBoundRate()).thenReturn(Ratio.ONE);
    when(exchangeRateTrackerMock.getUpperBoundRate()).thenReturn(Ratio.ONE);

    final EstimatedPaymentOutcome actual = amountTracker
      .setPaymentTarget(PaymentType.FIXED_DELIVERY, Ratio.ONE, UnsignedLong.ONE, BigInteger.ONE);

    assertThat(amountTracker.getAvailableDeliveryShortfall()).isEqualTo(UnsignedLong.ONE);
    assertThat(amountTracker.getSourceAmountInFlight()).isEqualTo(BigInteger.ZERO);
    assertThat(amountTracker.getDestinationAmountInFlight()).isEqualTo(BigInteger.ZERO);
    assertThat(amountTracker.getAmountSentInSourceUnits()).isEqualTo(BigInteger.ZERO);
    assertThat(amountTracker.getAmountDeliveredInDestinationUnits()).isEqualTo(BigInteger.ZERO);
    assertThat(amountTracker.getRemoteReceivedMax()).isEmpty();
    assertThat(amountTracker.getPaymentTargetConditions().get().paymentType()).isEqualTo(PaymentType.FIXED_DELIVERY);
    assertThat(amountTracker.getPaymentTargetConditions().get().maxPaymentAmountInSenderUnits())
      .isEqualTo(BigInteger.valueOf(2L));
    assertThat(amountTracker.getPaymentTargetConditions().get().minPaymentAmountInDestinationUnits())
      .isEqualTo(BigInteger.ONE);
    assertThat(amountTracker.getPaymentTargetConditions().get().minExchangeRate().toBigDecimal())
      .isEqualTo(BigDecimal.ONE);

    assertThat(actual.estimatedNumberOfPackets()).isEqualTo(BigInteger.valueOf(2L));
    assertThat(actual.maxSendAmountInWholeSourceUnits()).isEqualTo(BigInteger.valueOf(2L));
    assertThat(actual.maxSendAmountInWholeSourceUnits()).isEqualTo(BigInteger.valueOf(2L));
  }

  //////////////////////////
  // validateFxAndPacketSize
  //////////////////////////

  @Test
  public void testValidateFxAndPacketSizeWithNullLowerBound() {
    expectedException.expect(NullPointerException.class);
    amountTracker.validateFxAndPacketSize(null, Ratio.ONE, UnsignedLong.ONE);
  }

  @Test
  public void testValidateFxAndPacketSizeWithNullMinFxRate() {
    expectedException.expect(NullPointerException.class);
    amountTracker.validateFxAndPacketSize(Ratio.ONE, null, UnsignedLong.ONE);
  }

  @Test
  public void testValidateFxAndPacketSizeWithNullMaxPacketAmount() {
    expectedException.expect(NullPointerException.class);
    amountTracker.validateFxAndPacketSize(Ratio.ONE, Ratio.ONE, null);
  }

  @Test
  public void testValidateFxAndPacketSizeWithZeroAndZero() {
    amountTracker.validateFxAndPacketSize(
      Ratio.ZERO,
      Ratio.ZERO,
      UnsignedLong.ONE // <-- Use smallest packet size possible
    );
  }

  @Test
  public void testValidateFxAndPacketSizeWithZeroAndOne() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.InsufficientExchangeRate));
    expectedException.expectMessage(containsString(
      "Probed exchange-rate of 0/1[0] (floored to 0) is less-than than the minimum exchange-rate of "
        + "1/1[1] (ceiled to 1)"
    ));
    amountTracker.validateFxAndPacketSize(
      Ratio.ZERO,
      Ratio.ONE,
      UnsignedLong.MAX_VALUE // <-- Use largest packet size possible.
    );
  }

  @Test
  public void testValidateFxAndPacketSizeWithOneAndOne() {
    amountTracker.validateFxAndPacketSize(
      Ratio.ONE,
      Ratio.ONE,
      UnsignedLong.ONE // <-- Use smallest packet size possible
    );
  }

  @Test
  public void testValidateFxAndPacketSizeWithOneAndZero() {
    amountTracker.validateFxAndPacketSize(
      Ratio.ONE,
      Ratio.ZERO,
      UnsignedLong.ONE // <-- Use smallest packet size possible
    );
  }

  @Test
  public void testValidateFxAndPacketSizeLowestMaxPacketAmount1() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.ExchangeRateRoundingError));
    expectedException.expectMessage(containsString(
      "Rate enforcement may incur rounding errors. maxPacketAmount=999 is below proposed minimum of 1000"
    ));
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.101")), // Real
      Ratio.from(new BigDecimal("0.100")), // Min
      UnsignedLong.valueOf(999L) // <-- Requires at least 1000
    );
  }

  @Test
  public void testValidateFxAndPacketSizeLowestMaxPacketAmount2() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.ExchangeRateRoundingError));
    expectedException.expectMessage(containsString(
      "Rate enforcement may incur rounding errors. maxPacketAmount=999 is below proposed minimum of 1000"
    ));
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.200")), // Real
      Ratio.from(new BigDecimal("0.199")), // Min
      UnsignedLong.valueOf(999L) // <-- Requires at least 1000
    );
  }

  @Test
  public void testValidateFxAndPacketSizeLowestMaxPacketAmount3() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.ExchangeRateRoundingError));
    expectedException.expectMessage(containsString(
      "Rate enforcement may incur rounding errors. maxPacketAmount=999 is below proposed minimum of 1000"
    ));
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.499")), // Real
      Ratio.from(new BigDecimal("0.498")), // Min
      UnsignedLong.valueOf(999L) // <-- Requires at least 1000
    );
  }

  @Test
  public void testValidateFxAndPacketSizeLowestMaxPacketAmount4() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.ExchangeRateRoundingError));
    expectedException.expectMessage(containsString(
      "Rate enforcement may incur rounding errors. maxPacketAmount=999 is below proposed minimum of 1000"
    ));
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.500")), // Real
      Ratio.from(new BigDecimal("0.499")), // Min
      UnsignedLong.valueOf(999L) // <-- Requires at least 1000
    );
  }

  @Test
  public void testValidateFxAndPacketSizeLowestMaxPacketAmount5() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.ExchangeRateRoundingError));
    expectedException.expectMessage(containsString(
      "Rate enforcement may incur rounding errors. maxPacketAmount=999 is below proposed minimum of 1000"
    ));
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.999")), // Real
      Ratio.from(new BigDecimal("0.998")), // Min
      UnsignedLong.valueOf(999L) // <-- Requires at least 1000
    );
  }

  @Test
  public void testValidateFxAndPacketSizeLowestMaxPacketAmount6() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.InsufficientExchangeRate));
    expectedException.expectMessage(containsString(
      "Probed exchange-rate of 2/10[0.2] (floored to 0) is less-than than the minimum exchange-rate of "
        + "3/10[0.3] (ceiled to 1)"));
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.2")), // Real
      Ratio.from(new BigDecimal("0.3")), // Min
      UnsignedLong.MAX_VALUE.minus(UnsignedLong.ONE)
    );
  }

  @Test
  public void testValidateFxAndPacketSizeLowestMaxPacketAmount7() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.InsufficientExchangeRate));
    expectedException.expectMessage(containsString(
      "Probed exchange-rate of 10/10[1] (floored to 1) is less-than than the minimum exchange-rate of "
        + "13/10[1.3] (ceiled to 2)"
    ));
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("1.0")),
      Ratio.from(new BigDecimal("1.3")),
      UnsignedLong.ONE
    );
  }

  @Test
  public void testValidateFxAndPacketSizeLowestMaxPacketAmount8() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.InsufficientExchangeRate));
    expectedException.expectMessage(containsString(
      "Probed exchange-rate of 10/10[1] (floored to 1) is less-than than the minimum exchange-rate of "
        + "11/10[1.1] (ceiled to 2)"
    ));
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("1.0")),
      Ratio.from(new BigDecimal("1.1")),
      UnsignedLong.ONE
    );
  }

  @Test
  public void testValidateFxAndPacketSizeLowestMaxPacketAmount9() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.InsufficientExchangeRate));
    expectedException.expectMessage(containsString(
      "Probed exchange-rate of 10/10[1] (floored to 1) is less-than than the minimum exchange-rate of 11/10[1.1]"
    ));
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("1.0")),
      Ratio.from(new BigDecimal("1.1")),
      UnsignedLong.ONE
    );
  }

  @Test
  public void testValidateFxAndPacketSizeLowestMaxPacketAmount10() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.InsufficientExchangeRate));
    expectedException.expectMessage(containsString(
      "Probed exchange-rate of 10/10[1] (floored to 1) is less-than than the minimum exchange-rate of "
        + "11/10[1.1] (ceiled to 2)"
    ));
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("1.0")),
      Ratio.from(new BigDecimal("1.1")),
      UnsignedLong.ONE
    );
  }

  @Test
  public void testValidateFxAndPacketSizeLowestMaxPacketAmount11() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.InsufficientExchangeRate));
    expectedException.expectMessage(containsString(
      "Probed exchange-rate of 10/10[1] (floored to 1) is less-than than the minimum exchange-rate of "
        + "19/10[1.9] (ceiled to 2)"
    ));
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("1.0")),
      Ratio.from(new BigDecimal("1.9")),
      UnsignedLong.MAX_VALUE
    );
  }

  @Test
  public void testValidateFxAndPacketSizeLowestMaxPacketAmount12() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.InsufficientExchangeRate));
    expectedException.expectMessage(containsString(
      "Probed exchange-rate of 10/10[1] (floored to 1) is less-than than the minimum exchange-rate of "
        + "11/10[1.1] (ceiled to 2)"
    ));
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("1.0")), // Real
      Ratio.from(new BigDecimal("1.1")), // Min
      UnsignedLong.MAX_VALUE
    );
  }

  @Test
  public void testValidateFxAndPacketSizeLowestMaxPacketAmount13() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.InsufficientExchangeRate));
    expectedException.expectMessage(containsString(
      "Probed exchange-rate of 10/10[1] (floored to 1) is less-than than the minimum exchange-rate of "
        + "11/10[1.1] (ceiled to 2)"
    ));
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("1.0")), // Real
      Ratio.from(new BigDecimal("1.1")), // Min
      UnsignedLong.MAX_VALUE
    );
  }

  @Test
  public void testValidateFxAndPacketSizeLowestMaxPacketAmount14() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.InsufficientExchangeRate));
    expectedException.expectMessage(containsString(
      "Probed exchange-rate of 10/10[1] (floored to 1) is less-than than the minimum exchange-rate of "
        + "20/10[2] (ceiled to 2)"
    ));
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("1.0")),
      Ratio.from(new BigDecimal("2.0")),
      UnsignedLong.MAX_VALUE
    );
  }

  @Test
  public void testValidateFxAndPacketSizeLowestMaxPacketAmount15() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.ExchangeRateRoundingError));
    expectedException.expectMessage(containsString(
      "Rate enforcement may incur rounding errors. maxPacketAmount=1 is below proposed minimum of 10"
    ));
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("1.2")), // Real
      Ratio.from(new BigDecimal("1.1")), // Min
      UnsignedLong.ONE
    );
  }

  @Test
  public void testValidateFxAndPacketSizeLowestMaxPacketAmount16() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.ExchangeRateRoundingError));
    expectedException.expectMessage(containsString(
      "Rate enforcement may incur rounding errors. maxPacketAmount=1 is below proposed minimum of 3"
    ));
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("1.5")),
      Ratio.from(new BigDecimal("1.1")),
      UnsignedLong.ONE
    );
  }

  @Test
  public void testValidateFxAndPacketSizeLowestMaxPacketAmount17() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.InsufficientExchangeRate));
    expectedException.expectMessage(containsString(
      "Probed exchange-rate of 19/10[1.9] (floored to 1) is less-than than the minimum exchange-rate of "
        + "20/10[2] (ceiled to 2)"
    ));
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("1.9")),
      Ratio.from(new BigDecimal("2.0")),
      UnsignedLong.MAX_VALUE
    );
  }

  @Test
  public void testValidateFxAndPacketSizeLowestMaxPacketAmount18() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.InsufficientExchangeRate));
    expectedException.expectMessage(containsString(
      "Probed exchange-rate of 19/10[1.9] (floored to 1) is less-than than the minimum exchange-rate of "
        + "19/10[1.9] (ceiled to 2)"
    ));
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("1.9")),
      Ratio.from(new BigDecimal("1.9")),
      UnsignedLong.MAX_VALUE
    );
  }

  @Test
  public void testValidateFxAndPacketSizeLowestMaxPacketAmount19() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.InsufficientExchangeRate));
    expectedException.expectMessage(containsString(
      "Probed exchange-rate of 2/10[0.2] (floored to 0) is less-than than the minimum exchange-rate of "
        + "2/10[0.2] (ceiled to 1)"
    ));
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.20")),
      Ratio.from(new BigDecimal("0.20")),
      UnsignedLong.ONE
    );
  }

  @Test
  public void testValidateFxAndPacketSizeLowestMaxPacketAmount20() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.InsufficientExchangeRate));
    expectedException.expectMessage(containsString(
      "Probed exchange-rate of 501/1000[0.501] (floored to 0) is less-than than the minimum exchange-rate of "
        + "501/1000[0.501] (ceiled to 1)"
    ));
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.501")),
      Ratio.from(new BigDecimal("0.501")),
      UnsignedLong.ONE
    );
  }

  @Test
  public void testValidateFxAndPacketSizeLowestMaxPacketAmount21() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.InsufficientExchangeRate));
    expectedException.expectMessage(containsString(
      "Probed exchange-rate of 11/100[0.11] (floored to 0) is less-than than the minimum exchange-rate of "
        + "11/100[0.11] (ceiled to 1)"
    ));
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.11")),
      Ratio.from(new BigDecimal("0.11")),
      UnsignedLong.ONE
    );
  }

  @Test
  public void testValidateFxAndPacketSizeLowestMaxPacketAmount22() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.InsufficientExchangeRate));
    expectedException.expectMessage(containsString(
      "Probed exchange-rate of 2/10[0.2] (floored to 0) is less-than than the minimum exchange-rate of "
        + "2/10[0.2] (ceiled to 1)"
    ));
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.20")),
      Ratio.from(new BigDecimal("0.20")),
      UnsignedLong.ONE
    );
  }

  @Test
  public void testValidateFxAndPacketSizeLowestMaxPacketAmount23() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.InsufficientExchangeRate));
    expectedException.expectMessage(containsString(
      "Probed exchange-rate of 49/100[0.49] (floored to 0) is less-than than the minimum exchange-rate of "
        + "49/100[0.49] (ceiled to 1)"
    ));
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.49")),
      Ratio.from(new BigDecimal("0.49")),
      UnsignedLong.ONE
    );
  }

  @Test
  public void testValidateFxAndPacketSizeLowestMaxPacketAmount24() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.InsufficientExchangeRate));
    expectedException.expectMessage(containsString(
      "Probed exchange-rate of 5/10[0.5] (floored to 0) is less-than than the minimum exchange-rate of "
        + "5/10[0.5] (ceiled to 1)"
    ));
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.50")),
      Ratio.from(new BigDecimal("0.50")),
      UnsignedLong.ONE
    );
  }

  @Test
  public void testValidateFxAndPacketSizeLowestMaxPacketAmount25() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.InsufficientExchangeRate));
    expectedException.expectMessage(containsString(
      "Probed exchange-rate of 51/100[0.51] (floored to 0) is less-than than the minimum exchange-rate of "
        + "51/100[0.51] (ceiled to 1)"
    ));
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.51")),
      Ratio.from(new BigDecimal("0.51")),
      UnsignedLong.ONE
    );
  }

  @Test
  public void testValidateFxAndPacketSizeLowestMaxPacketAmount26() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.InsufficientExchangeRate));
    expectedException.expectMessage(containsString(
      "Probed exchange-rate of 99/100[0.99] (floored to 0) is less-than than the minimum exchange-rate of "
        + "99/100[0.99] (ceiled to 1)"
    ));
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.99")),
      Ratio.from(new BigDecimal("0.99")),
      UnsignedLong.ONE
    );
  }

  @Test
  public void testValidateExchangeRatesBetween0And1WithMargin1000Others() {
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.1")), // Real
      Ratio.from(new BigDecimal("0.05")), // Min
      UnsignedLong.MAX_VALUE
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.101")), // Real
      Ratio.from(new BigDecimal("0.100")), // Min
      UnsignedLong.MAX_VALUE
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.20")), // Real
      Ratio.from(new BigDecimal("0.19")), // Min
      UnsignedLong.MAX_VALUE
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.20")), // Real
      Ratio.from(new BigDecimal("0.199")), // Min
      UnsignedLong.MAX_VALUE
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.499")), // Real
      Ratio.from(new BigDecimal("0.498")), // Min
      UnsignedLong.MAX_VALUE
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.50")), // Real
      Ratio.from(new BigDecimal("0.49")), // Min
      UnsignedLong.MAX_VALUE
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.500")), // Real
      Ratio.from(new BigDecimal("0.499")), // Min
      UnsignedLong.valueOf(1000L)
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.501")), // Real
      Ratio.from(new BigDecimal("0.500")), // Min
      UnsignedLong.valueOf(1000L)
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.51")), // Real
      Ratio.from(new BigDecimal("0.50")), // Min
      UnsignedLong.valueOf(100L)
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.99")), // Real
      Ratio.from(new BigDecimal("0.98")), // Min
      UnsignedLong.valueOf(100L)
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.999")), // Real
      Ratio.from(new BigDecimal("0.998")), // Min
      UnsignedLong.valueOf(1000L)
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("20.0")),
      Ratio.from(new BigDecimal("1.9")),
      UnsignedLong.ONE
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("1.0")),
      Ratio.from(new BigDecimal("0.0")),
      UnsignedLong.ONE
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.builder().numerator(BigInteger.valueOf(242954600L)).denominator(BigInteger.valueOf(1000000000000L)).build(),
      Ratio.builder().numerator(BigInteger.valueOf(23080687L)).denominator(BigInteger.valueOf(100000000000L)).build(),
      UnsignedLong.valueOf(82320L)
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("2.0")),
      Ratio.from(new BigDecimal("1.9")),
      UnsignedLong.ONE
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("2.0")),
      Ratio.from(new BigDecimal("1.9")),
      UnsignedLong.MAX_VALUE
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("2.0")),
      Ratio.from(new BigDecimal("1.9")),
      UnsignedLong.valueOf(10L)
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("2.1")),
      Ratio.from(new BigDecimal("2.0")),
      UnsignedLong.ONE
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("2.1")),
      Ratio.from(new BigDecimal("1.9")),
      UnsignedLong.ONE
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("2.1")),
      Ratio.from(new BigDecimal("0.9")),
      UnsignedLong.MAX_VALUE
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("2.1")),
      Ratio.from(new BigDecimal("1.0")),
      UnsignedLong.MAX_VALUE
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("2.1")),
      Ratio.from(new BigDecimal("1.1")),
      UnsignedLong.MAX_VALUE
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("2.1")),
      Ratio.from(new BigDecimal("1.9")),
      UnsignedLong.MAX_VALUE
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("2.1")),
      Ratio.from(new BigDecimal("2.0")),
      UnsignedLong.MAX_VALUE
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("2.101")), // Real
      Ratio.from(new BigDecimal("1.100")), // Min
      UnsignedLong.MAX_VALUE
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.11")), // Real
      Ratio.from(new BigDecimal("0.10")), // Min
      UnsignedLong.MAX_VALUE
    );

    /////////////////
    // Real Probed Rate == .101
    /////////////////
    // Should work for any value 1000 or greater.
    for (long minPacketAmount = 1000; minPacketAmount < 2_000; minPacketAmount++) {
      amountTracker.validateFxAndPacketSize(
        Ratio.from(new BigDecimal("0.101")), // Real
        Ratio.from(new BigDecimal("0.100")), // Min
        UnsignedLong.valueOf(minPacketAmount)
      );
    }

    /////////////////
    // Real Probed Rate == .200
    /////////////////
    // Should work for any value 1000 or greater.
    for (long minPacketAmount = 1000; minPacketAmount < 2_000; minPacketAmount++) {
      amountTracker.validateFxAndPacketSize(
        Ratio.from(new BigDecimal("0.20")), // Real
        Ratio.from(new BigDecimal("0.199")), // Min
        UnsignedLong.valueOf(minPacketAmount)
      );
    }

    /////////////////
    // Real Probed Rate == .499
    /////////////////
    for (long minPacketAmount = 1000; minPacketAmount < 2_000; minPacketAmount++) {
      amountTracker.validateFxAndPacketSize(
        Ratio.from(new BigDecimal("0.499")), // Real
        Ratio.from(new BigDecimal("0.498")), // Min
        UnsignedLong.valueOf(minPacketAmount)
      );
    }

    /////////////////
    // Real Probed Rate == .500
    /////////////////
    for (long minPacketAmount = 1000; minPacketAmount < 2_000; minPacketAmount++) {
      amountTracker.validateFxAndPacketSize(
        Ratio.from(new BigDecimal("0.500")), // Real
        Ratio.from(new BigDecimal("0.499")), // Min
        UnsignedLong.valueOf(minPacketAmount)
      );
    }

    /////////////////
    // Real Probed Rate == .501
    /////////////////
    for (long minPacketAmount = 1000; minPacketAmount < 2_000; minPacketAmount++) {
      amountTracker.validateFxAndPacketSize(
        Ratio.from(new BigDecimal("0.501")), // Real
        Ratio.from(new BigDecimal("0.500")), // Min
        UnsignedLong.valueOf(minPacketAmount)
      );
    }

    /////////////////
    // Real Probed Rate == .999
    /////////////////
    for (long minPacketAmount = 1000; minPacketAmount < 2_000; minPacketAmount++) {
      amountTracker.validateFxAndPacketSize(
        Ratio.from(new BigDecimal("0.999")), // Real
        Ratio.from(new BigDecimal("0.998")), // Min
        UnsignedLong.valueOf(minPacketAmount)
      );
    }

    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("1.0")), // Real
      Ratio.from(new BigDecimal("0.999")), // Min
      UnsignedLong.valueOf(1L)
    );
  }

  @Test
  public void testValidateExchangeRatesBetween0And1WithMargin100() {
    // Should work for any value 100 or greater.
    for (long min = 100; min < 1_000; min++) {
      amountTracker.validateFxAndPacketSize(
        Ratio.from(new BigDecimal("0.11")), // Real
        Ratio.from(new BigDecimal("0.10")), // Min
        UnsignedLong.valueOf(min)
      );
    }

    /////////////////
    // Real Probed Rate == .101
    /////////////////

    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.11")), // Real
      Ratio.from(new BigDecimal("0.10")), // Min
      UnsignedLong.valueOf(100L)
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.11")), // Real
      Ratio.from(new BigDecimal("0.10")), // Min
      UnsignedLong.valueOf(101L) // Will work for 1000 or greater
    );

    /////////////////
    // Real Probed Rate == .20
    /////////////////

    // Requires Max of at least 100
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.20")), // Real
      Ratio.from(new BigDecimal("0.19")), // Min
      UnsignedLong.valueOf(100L)
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.21")), // Real
      Ratio.from(new BigDecimal("0.20")), // Min
      UnsignedLong.valueOf(101L) // Will work for 1000 or greater
    );

    /////////////////
    // Real Probed Rate == .25
    /////////////////
    for (int i = 1000; i < 2000; i++) {
      amountTracker.validateFxAndPacketSize(
        Ratio.from(new BigDecimal("0.250")), // Real
        Ratio.from(new BigDecimal("0.249")), // Min
        UnsignedLong.valueOf(i)
      );
    }

    /////////////////
    // Real Probed Rate == .49
    /////////////////

    // Requires Max of at least 1000
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.49")), // Real
      Ratio.from(new BigDecimal("0.48")), // Min
      UnsignedLong.valueOf(100L)
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.49")), // Real
      Ratio.from(new BigDecimal("0.48")), // Min
      UnsignedLong.valueOf(101L) // Will work for 1000 or greater
    );

    /////////////////
    // Real Probed Rate == .500
    /////////////////

    // Requires Max of at least 1000
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.50")), // Real
      Ratio.from(new BigDecimal("0.49")), // Min
      UnsignedLong.valueOf(100L)
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.50")), // Real
      Ratio.from(new BigDecimal("0.49")), // Min
      UnsignedLong.valueOf(101L) // Will work for 1000 or greater
    );

    /////////////////
    // Real Probed Rate == .501
    /////////////////

    // Requires Max of at least 1000
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.51")), // Real
      Ratio.from(new BigDecimal("0.50")), // Min
      UnsignedLong.valueOf(100L)
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.51")), // Real
      Ratio.from(new BigDecimal("0.50")), // Min
      UnsignedLong.valueOf(101L) // Will work for 1000 or greater
    );

    /////////////////
    // Real Probed Rate == .99
    /////////////////

    // Requires Max of at least 1000
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.99")), // Real
      Ratio.from(new BigDecimal("0.98")), // Min
      UnsignedLong.valueOf(100L)
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.99")), // Real
      Ratio.from(new BigDecimal("0.98")), // Min
      UnsignedLong.valueOf(101L) // Will work for 1000 or greater
    );
  }

  @Test
  public void testValidateExchangeRatesBetween0And1WithMargin10() {
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.1")), // Real
      Ratio.from(new BigDecimal("0.05")), // Min
      UnsignedLong.valueOf(100L)
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.1")), // Real
      Ratio.from(new BigDecimal("0.05")), // Min
      UnsignedLong.valueOf(101L) // Will work for 1000 or greater
    );
  }

  @Test
  public void testValidateExchangeRatesBetween1And2WithMargin1000() {
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("1.2")), // Real
      Ratio.from(new BigDecimal("1.1")), // Min
      UnsignedLong.valueOf(10L) // Sufficient for the difference.
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("2.101")), // Real
      Ratio.from(new BigDecimal("1.101")), // Min
      UnsignedLong.valueOf(1L)
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("2.0")), // Real
      Ratio.from(new BigDecimal("1.0")), // Min
      UnsignedLong.valueOf(1L)
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("2.101")), // Real
      Ratio.from(new BigDecimal("1.100")), // Min
      UnsignedLong.valueOf(1000L) // Will work for 1000 or greater
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("2.101")), // Real
      Ratio.from(new BigDecimal("1.100")), // Min
      UnsignedLong.valueOf(1001L) // Will work for 1000 or greater
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("2")), // Real
      Ratio.from(new BigDecimal("0.999")), // Min
      UnsignedLong.valueOf(1L)
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("10.20")), // Real
      Ratio.from(new BigDecimal("0.199")), // Min
      UnsignedLong.valueOf(1L)
    );
  }

  @Test
  public void testEqualInfiniteFractions() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.InsufficientExchangeRate));
    expectedException.expectMessage(containsString(
      "Probed exchange-rate of 4/3[1.333333333333333333333333333333333] (floored to 1) is less-than than the"
        + " minimum exchange-rate of 4/3[1.333333333333333333333333333333333] (ceiled to 2)"
    ));

    amountTracker.validateFxAndPacketSize(
      Ratio.builder().numerator(BigInteger.valueOf(4L)).denominator(BigInteger.valueOf(3L)).build(), // Real
      Ratio.builder().numerator(BigInteger.valueOf(4L)).denominator(BigInteger.valueOf(3L)).build(), // Min
      UnsignedLong.valueOf(10L)
    );
  }

  /**
   * @see "https://github.com/interledgerjs/interledgerjs/issues/167#issuecomment-745615676"
   */
  @Test
  public void testValidateFxAndPacketSizeForGithubExampleTooSmall() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.ExchangeRateRoundingError));
    expectedException.expectMessage(containsString(
      "Rate enforcement may incur rounding errors. maxPacketAmount=205799 is below proposed minimum of 205800"
    ));
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.000242954600")), // Real
      Ratio.from(new BigDecimal("0.000238095508")), // Min
      UnsignedLong.valueOf(205_799) // <-- One unit too small.
    );
  }

  /**
   * @see "https://github.com/interledgerjs/interledgerjs/issues/167#issuecomment-745615676"
   */
  @Test
  public void testValidateFxAndPacketSizeForGithubExampleJustRight() {
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.000242954600")), // Real
      Ratio.from(new BigDecimal("0.000238095508")), // Min
      UnsignedLong.valueOf(205_800) // <-- One unit too small.
    );
  }

  /**
   * @see "https://github.com/interledgerjs/interledgerjs/issues/167#issuecomment-745615676"
   */
  @Test
  public void testValidateFxAndPacketSizeForGithubExampleBigger() {
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.000242954600")), // Real
      Ratio.from(new BigDecimal("0.000238095508")), // Min
      UnsignedLong.valueOf(205_801) // <-- One unit too small.
    );
  }

  /**
   * @see "https://github.com/interledgerjs/interledgerjs/issues/167#issuecomment-745615676"
   */
  @Test
  public void testValidateFxAndPacketSizeForGithubExampleTooBig() {
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.000242954600")), // Real
      Ratio.from(new BigDecimal("0.000238095508")), // Min
      UnsignedLong.MAX_VALUE // <-- One unit too small.
    );
  }

  @Test
  public void testValidateExchangeRatesMisc() {
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("1.91")), // Real
      Ratio.from(new BigDecimal("1.90")), // Min
      UnsignedLong.valueOf(100L)
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("1.95")), // Real
      Ratio.from(new BigDecimal("1.90")), // Min
      UnsignedLong.valueOf(20L)
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("2.00")), // Real
      Ratio.from(new BigDecimal("1.90")), // Min
      UnsignedLong.valueOf(10L)
    );

    // All maxPacketAmounts above 19 should work.
    for (int i = 10; i < 100; i++) {
      amountTracker.validateFxAndPacketSize(
        Ratio.from(new BigDecimal("2.00")), // Real
        Ratio.from(new BigDecimal("1.90")), // Min
        UnsignedLong.valueOf(i)
      );
    }

    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.0002429546")), // Real
      Ratio.from(new BigDecimal("0.0002429545")), // Min
      UnsignedLong.valueOf(10000000000L)
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("0.000242954600")), // Real
      Ratio.from(new BigDecimal("0.000238095508")), // Min
      UnsignedLong.valueOf(205_800)
    );
  }

  @Test
  public void testValidateExchnageRatesWithSmallMax() {
    amountTracker.validateFxAndPacketSize(Ratio.ZERO, Ratio.ZERO, UnsignedLong.ONE);
    amountTracker.validateFxAndPacketSize(Ratio.ONE, Ratio.ONE, UnsignedLong.ONE);
    amountTracker.validateFxAndPacketSize(Ratio.ONE, Ratio.ZERO, UnsignedLong.ONE);

    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("1.0")),
      Ratio.from(new BigDecimal("0.0")),
      UnsignedLong.ONE
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("2.1")),
      Ratio.from(new BigDecimal("2.0")),
      UnsignedLong.valueOf(10L)
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("2.1")),
      Ratio.from(new BigDecimal("1.9")),
      UnsignedLong.valueOf(5L)
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("2.1")),
      Ratio.from(new BigDecimal("1.1")),
      UnsignedLong.ONE
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("2.1")),
      Ratio.from(new BigDecimal("1.0")),
      UnsignedLong.ONE
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("2.1")),
      Ratio.from(new BigDecimal("0.9")),
      UnsignedLong.ONE
    );
    amountTracker.validateFxAndPacketSize(
      Ratio.from(new BigDecimal("20.0")),
      Ratio.from(new BigDecimal("1.9")),
      UnsignedLong.ONE
    );
  }
}