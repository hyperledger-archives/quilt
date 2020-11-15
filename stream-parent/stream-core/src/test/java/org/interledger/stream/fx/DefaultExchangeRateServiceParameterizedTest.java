package org.interledger.stream.fx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.common.primitives.UnsignedLong;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collection;
import javax.money.Monetary;
import javax.money.convert.ConversionContext;
import javax.money.convert.ExchangeRate;
import javax.money.convert.ExchangeRateProvider;
import org.interledger.fx.Denomination;
import org.javamoney.moneta.convert.ExchangeRateBuilder;
import org.javamoney.moneta.spi.DefaultNumberValue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link DefaultExchangeRateService}.
 */
@RunWith(Parameterized.class)
public class DefaultExchangeRateServiceParameterizedTest {

  @Parameter
  public double senderPrice;
  @Parameter(1)
  public double receiverPrice;
  @Parameter(2)
  public long senderAmount;
  @Parameter(3)
  public String senderCode;
  @Parameter(4)
  public int senderScale;
  @Parameter(5)
  public String receiverCode;
  @Parameter(6)
  public int receiverScale;
  @Parameter(7)
  public double slippage;
  @Parameter(8)
  public long expectedResult;

  @Mock
  private ExchangeRateProvider exchangeRateProvider;

  private ExchangeRateService exchangeRateService;

  @Parameters
  public static Collection primeNumbers() {
    return Arrays.asList(new Object[][]{
      // [0] Amount gets larger
      {6.0, 1.5, 100, "ABC", 2, "XYZ", 2, 0.0, 400},
      // [1] Amount gets smaller
      {1.5, 6.0, 100, "ABC", 2, "XYZ", 2, 0.0, 25},
      // [2] Converts from small to large scale
      {1.0, 1.0, 33, "ABC", 2, "XYZ", 6, 0.0, 330_000},
      // [3] Converts from large to small scale
      {1.0, 1.0, 123_456_000_000L, "ABC", 9, "XYZ", 4, 0.0, 1_234_560},
      // [4] Subtracts slippage in simple case
      {1.0, 1.0, 100, "ABC", 2, "XYZ", 2, 0.01, 99},
      // [5] Rounds up after subtracting slippage
      {1.0, 1.0, 100, "ABC", 2, "XYZ", 2, 0.035, 97},
      // [6] Rounds up even when destination amount is very close to 0
      {0.000_000_5, 1.0, 100, "ABC", 0, "XYZ", 0, 0.0, 1},
      // [7] UL multiplication errors would cause this to be 101 after rounding up, BigDecimal fixes this
      {1.0, 1.0, 100, "ABC", 9, "XYZ", 9, 0.0, 100},
      // [8] Converts when using the largest possible scale
      {1.0, 1.0, 421, "ABC", 255, "XYZ", 255, 0.0, 421},
      // [9] Test scale with FX of 2:1
      {2.0, 1.0, 1, "ABC", 2, "XYZ", 2, 0.0, 2},
      // [10] Test scale with FX of 1:2
      {1.0, 2.0, 1, "ABC", 2, "XYZ", 2, 0.0, 1},
      // [11] Test scale with slippage at lower bound.
      {1.0, 1.0, 1, "ABC", 2, "XYZ", 2, 0.015, 1},
      // [12] Test scale with slippage at higher bound.
      {1.0, 1.0, 100, "ABC", 2, "XYZ", 2, 0.015, 99},
      // [13] Test scale with slippage at higher bound.
      {1.0, 1035, 100, "ABC", 6, "XYZ", 9, 0.0, 97},
      // [14] Test scale with slippage at higher bound.
      {1.0, 1035.0, 2000, "ABC", 6, "XYZ", 9, 0.0, 1933},
      // [15] Test scale with slippage at higher bound.
      {4_000_000, 1, 1, "ABC", 0, "XYZ", 6, 0.0, 4_000_000_000_000L},
    });
  }


  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    this.populateTestRates();

    exchangeRateService = new DefaultExchangeRateService(exchangeRateProvider);
  }

  private void populateTestRates() {
    ExchangeRate usdToUsdRate = new ExchangeRateBuilder(ConversionContext.HISTORIC_CONVERSION)
      .setBase(Monetary.getCurrency("USD"))
      .setTerm(Monetary.getCurrency("USD"))
      .setFactor(DefaultNumberValue.of(new BigDecimal("1.0")))
      .build();
    when(exchangeRateProvider.getExchangeRate("USD", "USD")).thenReturn(usdToUsdRate);

    ExchangeRate usdToEurRate = new ExchangeRateBuilder(ConversionContext.HISTORIC_CONVERSION)
      .setBase(Monetary.getCurrency("USD"))
      .setTerm(Monetary.getCurrency("EUR"))
      .setFactor(DefaultNumberValue.of(new BigDecimal("2.0")))
      .build();
    when(exchangeRateProvider.getExchangeRate("USD", "EUR")).thenReturn(usdToEurRate);

    ExchangeRate eurToUsdRate = new ExchangeRateBuilder(ConversionContext.HISTORIC_CONVERSION)
      .setBase(Monetary.getCurrency("EUR"))
      .setTerm(Monetary.getCurrency("USD"))
      .setFactor(DefaultNumberValue.of(new BigDecimal("0.5")))
      .build();
    when(exchangeRateProvider.getExchangeRate("EUR", "USD")).thenReturn(eurToUsdRate);

    // XRP to ETH
    ExchangeRate xrpToEthRate = new ExchangeRateBuilder(ConversionContext.HISTORIC_CONVERSION)
      .setBase(Monetary.getCurrency("MXN"))
      .setTerm(Monetary.getCurrency("CAD"))
      .setFactor(DefaultNumberValue.of(new BigDecimal("1.03555")))
      .build();
    when(exchangeRateProvider.getExchangeRate("MXN", "CAD")).thenReturn(xrpToEthRate);

//    // 4 XRP to 1 USD
//    ExchangeRate xrpToUsdRate = new ExchangeRateBuilder(ConversionContext.HISTORIC_CONVERSION)
//        .setBase(Monetary.getCurrency("MXN"))
//        .setTerm(Monetary.getCurrency("USD"))
//        .setFactor(DefaultNumberValue.of(new BigDecimal("4.0")))
//        .build();
//    when(exchangeRateProvider.getExchangeRate("MXN", "USD")).thenReturn(xrpToUsdRate);

    // 4_000_000 XRP Drops to 1 USD
    ExchangeRate xrpToUsdRate = new ExchangeRateBuilder(ConversionContext.HISTORIC_CONVERSION)
      .setBase(Monetary.getCurrency("USD"))
      .setTerm(Monetary.getCurrency("MXN"))
      .setFactor(DefaultNumberValue.of(new BigDecimal("4.0")))
      .build();
    when(exchangeRateProvider.getExchangeRate("USD", "MXN")).thenReturn(xrpToUsdRate);
  }

  /**
   * This test will run for all parameterized values.
   */
  @Test
  public void testExpectedConversions() {
    final UnsignedLong sourceAmount = UnsignedLong.valueOf(senderAmount);
    ExchangeRate rate = new ExchangeRateBuilder(ConversionContext.HISTORIC_CONVERSION)
      .setBase(Monetary.getCurrency(senderCode))
      .setTerm(Monetary.getCurrency(receiverCode))
      .setFactor(DefaultNumberValue
        .of(new BigDecimal(senderPrice).divide(new BigDecimal(receiverPrice), 20, RoundingMode.DOWN)))
      .build();
    when(exchangeRateProvider.getExchangeRate(senderCode, receiverCode)).thenReturn(rate);

    Denomination senderDenomination = Denomination.builder()
      .assetCode(senderCode)
      .assetScale((short) senderScale)
      .build();
    Denomination receiverDenomination = Denomination.builder()
      .assetCode(receiverCode)
      .assetScale((short) receiverScale)
      .build();
    final BigDecimal slippagePercent = BigDecimal.valueOf(slippage);

    final BigDecimal scaledExchangeRate = exchangeRateService
      .getScaledExchangeRate(senderDenomination, receiverDenomination, slippagePercent);

    UnsignedLong actual = exchangeRateService.convert(sourceAmount, scaledExchangeRate);
    assertThat(actual).isEqualTo(UnsignedLong.valueOf(expectedResult));
  }

}
