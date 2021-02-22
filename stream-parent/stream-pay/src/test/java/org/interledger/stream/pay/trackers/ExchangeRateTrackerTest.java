package org.interledger.stream.pay.trackers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.stream.pay.StreamPayerExceptionMatcher.hasSendState;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.interledger.core.fluent.Ratio;
import org.interledger.fx.Denominations;
import org.interledger.fx.OracleExchangeRateService;
import org.interledger.stream.pay.exceptions.StreamPayerException;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.probing.model.DeliveredExchangeRateBound;

import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

/**
 * Unit tests for {@link ExchangeRateTracker}.
 */
public class ExchangeRateTrackerTest {

  private static final UnsignedLong ONE = UnsignedLong.ONE;
  private static final UnsignedLong TEN = UnsignedLong.valueOf(10L);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private OracleExchangeRateService oracleExchangeRateServiceMock;

  private ExchangeRateTracker exchangeRateTracker;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    this.exchangeRateTracker = new ExchangeRateTracker(oracleExchangeRateServiceMock);
  }

  //////////////////////////
  // rateBounds
  //////////////////////////

  @Test
  public void getLowerRateBoundNull() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.RateProbeFailed));

    exchangeRateTracker.setRateBounds(null, Ratio.from(new BigDecimal("0.02")));

    exchangeRateTracker.getLowerBoundRate();
  }

  @Test
  public void getUpperRateBoundNull() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.RateProbeFailed));

    exchangeRateTracker.setRateBounds(Ratio.from(new BigDecimal("0.02")), null);

    exchangeRateTracker.getUpperBoundRate();
  }

  @Test
  public void getRateBounds() {
    Ratio lowerBound = Ratio.from(new BigDecimal("0.01"));
    Ratio upperBound = Ratio.from(new BigDecimal("0.02"));

    exchangeRateTracker.setRateBounds(lowerBound, upperBound);

    assertThat(exchangeRateTracker.getLowerBoundRate()).isEqualTo(lowerBound);
    assertThat(exchangeRateTracker.getUpperBoundRate()).isEqualTo(upperBound);
  }

  //////////////////////////
  // updateRate
  //////////////////////////

  @Test
  public void updateRateWithNullSourceAmount() {
    expectedException.expect(NullPointerException.class);
    exchangeRateTracker.updateRate(null, UnsignedLong.ZERO);
  }

  @Test
  public void updateRateWithNullDestAmount() {
    expectedException.expect(NullPointerException.class);
    exchangeRateTracker.updateRate(UnsignedLong.ZERO, null);
  }

  @Test
  public void updateRateWithEmptyReceivedAmountsAndNullBounds() {
    exchangeRateTracker.updateRate(ONE, TEN);

    assertThat(exchangeRateTracker.getLowerBoundRate().toBigDecimal()).isEqualTo(BigDecimal.TEN);
    assertThat(exchangeRateTracker.getUpperBoundRate().toBigDecimal()).isEqualTo(BigDecimal.TEN.add(BigDecimal.ONE));
  }

  @Test
  public void updateRateWithEmptyReceivedAmountsAndNullLowerBounds() {
    exchangeRateTracker.setRateBounds(null, Ratio.from(BigDecimal.TEN));
    exchangeRateTracker.updateRate(ONE, TEN);

    assertThat(exchangeRateTracker.getLowerBoundRate().toBigDecimal()).isEqualTo(BigDecimal.TEN);
    assertThat(exchangeRateTracker.getUpperBoundRate().toBigDecimal()).isEqualTo(BigDecimal.TEN);
  }

  @Test
  public void updateRateWithEmptyReceivedAmountsAndNullUpperBounds() {
    exchangeRateTracker.setRateBounds(Ratio.from(BigDecimal.TEN), null);
    exchangeRateTracker.updateRate(ONE, TEN);

    assertThat(exchangeRateTracker.getLowerBoundRate().toBigDecimal()).isEqualTo(BigDecimal.TEN);
    assertThat(exchangeRateTracker.getUpperBoundRate().toBigDecimal()).isEqualTo(BigDecimal.TEN.add(BigDecimal.ONE));
  }

  @Test
  public void updateRateWithEmptyReceivedAmountsUpperAndLowerGreaterBounds() {
    exchangeRateTracker.setRateBounds(Ratio.from(BigDecimal.ONE), Ratio.from(BigDecimal.valueOf(4L)));

    exchangeRateTracker.updateRate(ONE, UnsignedLong.valueOf(2L));

    assertThat(exchangeRateTracker.getLowerBoundRate().toBigDecimal()).isEqualTo(BigDecimal.valueOf(2L));
    assertThat(exchangeRateTracker.getUpperBoundRate().toBigDecimal()).isEqualTo(BigDecimal.valueOf(3L));
  }

  @Test
  public void updateRateWithReceivedAmounts() {
    this.exchangeRateTracker = new ExchangeRateTracker(oracleExchangeRateServiceMock) {
      @Override
      boolean shouldResetExchangeRate(
        Ratio packetLowerBoundRate, Ratio packetUpperBoundRate,
        UnsignedLong previousReceivedAmount, UnsignedLong receivedAmount
      ) {
        return true;
      }
    };
    final UnsignedLong sourceAmount = ONE;
    final UnsignedLong receivedAmount = TEN;
    exchangeRateTracker.getReceivedAmounts().put(sourceAmount, receivedAmount);

    exchangeRateTracker.updateRate(sourceAmount, receivedAmount);

    assertThat(exchangeRateTracker.getLowerBoundRate().toBigDecimal()).isEqualTo(BigDecimal.valueOf(10L));
    assertThat(exchangeRateTracker.getUpperBoundRate().toBigDecimal()).isEqualTo(BigDecimal.valueOf(11));
    assertThat(exchangeRateTracker.getSentAmounts().size()).isEqualTo(1);
    assertThat(exchangeRateTracker.getReceivedAmounts().size()).isEqualTo(1);
  }

  @Test
  public void updateRateWhenReset() {
    this.exchangeRateTracker = new ExchangeRateTracker(oracleExchangeRateServiceMock) {
      @Override
      boolean shouldResetExchangeRate(
        Ratio packetLowerBoundRate, Ratio packetUpperBoundRate,
        UnsignedLong previousReceivedAmount, UnsignedLong receivedAmount
      ) {
        return true;
      }
    };
    exchangeRateTracker.getReceivedAmounts().put(ONE, TEN);
    exchangeRateTracker.updateRate(ONE, UnsignedLong.ONE);

    assertThat(exchangeRateTracker.getLowerBoundRate().toBigDecimal()).isEqualTo(BigDecimal.ONE);
    assertThat(exchangeRateTracker.getUpperBoundRate().toBigDecimal()).isEqualTo(BigDecimal.valueOf(2L));
    assertThat(exchangeRateTracker.getSentAmounts().size()).isEqualTo(1);
    assertThat(exchangeRateTracker.getReceivedAmounts().size()).isEqualTo(1);
  }

  ////////////////////////
  // estimateDestinationAmount
  ////////////////////////

  @Test
  public void estimateDestinationAmountWithNull() {
    expectedException.expect(NullPointerException.class);
    exchangeRateTracker.estimateDestinationAmount(null);
  }

  @Test
  public void estimateDestinationAmountWithReceivedAmount() {
    UnsignedLong sourceAmount = UnsignedLong.ONE;
    UnsignedLong received = UnsignedLong.MAX_VALUE;
    exchangeRateTracker.getReceivedAmounts().put(sourceAmount, received);

    DeliveredExchangeRateBound actual = exchangeRateTracker.estimateDestinationAmount(sourceAmount);

    assertThat(actual.lowEndEstimate()).isEqualTo(UnsignedLong.MAX_VALUE);
    assertThat(actual.highEndEstimate()).isEqualTo(UnsignedLong.MAX_VALUE);
  }

  @Test
  public void estimateDestinationAmountWithoutReceivedAmount() {
    UnsignedLong sourceAmount = UnsignedLong.valueOf(3L);
    Ratio lowerBoundRate = Ratio.from(BigDecimal.valueOf(2L));
    Ratio upperBoundRate = Ratio.from(BigDecimal.valueOf(4L));
    exchangeRateTracker.setRateBounds(lowerBoundRate, upperBoundRate);

    DeliveredExchangeRateBound actual = exchangeRateTracker.estimateDestinationAmount(sourceAmount);

    assertThat(actual.lowEndEstimate()).isEqualTo(UnsignedLong.valueOf(6L));
    assertThat(actual.highEndEstimate()).isEqualTo(UnsignedLong.valueOf(11L));
  }

  @Test
  public void estimateDestinationAmountWithoutReceivedAmountAndNoBounds() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.RateProbeFailed));
    UnsignedLong sourceAmount = UnsignedLong.valueOf(3L);
    DeliveredExchangeRateBound actual = exchangeRateTracker.estimateDestinationAmount(sourceAmount);

    assertThat(actual.lowEndEstimate()).isEqualTo(UnsignedLong.valueOf(6L));
    assertThat(actual.highEndEstimate()).isEqualTo(UnsignedLong.valueOf(11L));
  }

  ////////////////////////
  // shouldResetExchangeRate
  ////////////////////////

  @Test
  public void shouldResetExchangeRatePreviousNotReceived() {
    exchangeRateTracker.updateRate(UnsignedLong.ONE, UnsignedLong.ONE);
    assertThat(exchangeRateTracker.shouldResetExchangeRate(
      Ratio.ONE, Ratio.ONE, UnsignedLong.ONE, UnsignedLong.ONE
    )).isTrue();
  }

  @Test
  public void shouldResetExchangeRateUpperLessThanLower() {
    Ratio lowerBoundRate = Ratio.from(BigDecimal.valueOf(3L));
    Ratio upperBoundRate = Ratio.ONE;
    exchangeRateTracker.setRateBounds(lowerBoundRate, upperBoundRate);

    assertThat(exchangeRateTracker.shouldResetExchangeRate(
      // Lower is equal, Upper is less
      Ratio.ONE, Ratio.from(BigDecimal.valueOf(2)), UnsignedLong.ONE, UnsignedLong.MAX_VALUE
    )).isTrue();
  }

  @Test
  public void shouldResetExchangeRateLowerGreaterThanUpper() {
    Ratio lowerBoundRate = Ratio.ONE;
    Ratio upperBoundRate = Ratio.from(BigDecimal.valueOf(2L));
    exchangeRateTracker.setRateBounds(lowerBoundRate, upperBoundRate);

    assertThat(exchangeRateTracker.shouldResetExchangeRate(
      // Lower is greater, Upper is Equal
      Ratio.from(BigDecimal.valueOf(3)), Ratio.from(BigDecimal.valueOf(2L)), UnsignedLong.ONE, UnsignedLong.MAX_VALUE
    )).isTrue();
  }

  //////////////////
  // InitializeRates
  //////////////////

  @Test
  public void initializeRatesWithNullSource() {
    expectedException.expect(NullPointerException.class);
    exchangeRateTracker.initializeRates(null, Denominations.USD);
  }

  @Test
  public void initializeRatesWithNullDest() {
    expectedException.expect(NullPointerException.class);
    exchangeRateTracker.initializeRates(Denominations.USD, null);
  }

  @Test
  public void initializeRatesWithSame() {
    expectedException.expect(NullPointerException.class);

    exchangeRateTracker.initializeRates(Denominations.USD, Denominations.USD);

    assertThat(exchangeRateTracker.getLowerBoundRate()).isEqualTo(Ratio.ONE);
    assertThat(exchangeRateTracker.getUpperBoundRate()).isEqualTo(Ratio.ONE);
    assertThat(exchangeRateTracker.getSentAmounts().size()).isEqualTo(1L);
    assertThat(exchangeRateTracker.getReceivedAmounts().size()).isEqualTo(1L);

    verify(oracleExchangeRateServiceMock).getExchangeRate(Mockito.any(), Mockito.any());
    verifyNoInteractions(oracleExchangeRateServiceMock);
  }

  @Test
  public void initializeRatesWithDifferent() {
    expectedException.expect(NullPointerException.class);

    exchangeRateTracker.initializeRates(Denominations.USD, Denominations.XRP_DROPS);

    assertThat(exchangeRateTracker.getLowerBoundRate()).isEqualTo(Ratio.ONE);
    assertThat(exchangeRateTracker.getUpperBoundRate()).isEqualTo(Ratio.ONE);
    assertThat(exchangeRateTracker.getSentAmounts().size()).isEqualTo(1L);
    assertThat(exchangeRateTracker.getReceivedAmounts().size()).isEqualTo(1L);

    verify(oracleExchangeRateServiceMock).getExchangeRate(Mockito.any(), Mockito.any());
    verifyNoInteractions(oracleExchangeRateServiceMock);
  }

  @Test
  public void initializeRatesWithDifferentTwice() {
    expectedException.expect(NullPointerException.class);

    exchangeRateTracker.initializeRates(Denominations.USD, Denominations.XRP_DROPS);
    exchangeRateTracker.initializeRates(Denominations.USD, Denominations.XRP_DROPS);

    assertThat(exchangeRateTracker.getLowerBoundRate()).isEqualTo(Ratio.ONE);
    assertThat(exchangeRateTracker.getUpperBoundRate()).isEqualTo(Ratio.ONE);
    assertThat(exchangeRateTracker.getSentAmounts().size()).isEqualTo(1L);
    assertThat(exchangeRateTracker.getReceivedAmounts().size()).isEqualTo(1L);

    verify(oracleExchangeRateServiceMock, times(2)).getExchangeRate(Mockito.any(), Mockito.any());
    verifyNoInteractions(oracleExchangeRateServiceMock);
  }
}