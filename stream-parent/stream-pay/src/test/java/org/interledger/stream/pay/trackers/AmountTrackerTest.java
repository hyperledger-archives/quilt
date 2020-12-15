package org.interledger.stream.pay.trackers;

import org.interledger.core.fluent.Ratio;
import org.interledger.stream.pay.exceptions.StreamPayerException;

import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

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