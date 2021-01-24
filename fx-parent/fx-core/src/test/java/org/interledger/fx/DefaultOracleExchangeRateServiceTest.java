package org.interledger.fx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.interledger.core.fluent.Ratio;

import org.javamoney.moneta.spi.DefaultNumberValue;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import javax.money.convert.ExchangeRate;
import javax.money.convert.ExchangeRateProvider;

/**
 * Unit tests for {@link DefaultOracleExchangeRateService}.
 */
public class DefaultOracleExchangeRateServiceTest {

  @Mock
  private ExchangeRate exchangeRateMock;

  @Mock
  private ExchangeRateProvider exchangeRateProvider;

  private OracleExchangeRateService oracleExchangeRateService;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    oracleExchangeRateService = new DefaultOracleExchangeRateService(exchangeRateProvider);
  }

  @Test
  public void testSourceLessThanDest() {
    when(exchangeRateMock.getFactor()).thenReturn(DefaultNumberValue.of(new BigDecimal("1.0")));
    when(exchangeRateProvider.getExchangeRate(anyString(), anyString())).thenReturn(exchangeRateMock);

    ScaledExchangeRate actual = oracleExchangeRateService.getScaledExchangeRate(
      Denomination.builder().assetCode("SRC").assetScale((short) 9).build(),
      Denomination.builder().assetCode("DST").assetScale((short) 6).build(),
      Slippage.NONE
    );
    assertThat(actual.value()).isEqualTo(Ratio.from(BigDecimal.valueOf(0.001)));

    actual = oracleExchangeRateService.getScaledExchangeRate(
      Denomination.builder().assetCode("SRC").assetScale((short) 9).build(),
      Denomination.builder().assetCode("DST").assetScale((short) 6).build(),
      Slippage.ONE_PERCENT
    );
    assertThat(actual.value()).isEqualTo(Ratio.from(BigDecimal.valueOf(0.00099)));
  }

  @Test
  public void testSourceGreaterThanDest() {
    when(exchangeRateMock.getFactor()).thenReturn(DefaultNumberValue.of(new BigDecimal("1.0")));
    when(exchangeRateProvider.getExchangeRate(anyString(), anyString())).thenReturn(exchangeRateMock);

    ScaledExchangeRate actual = oracleExchangeRateService.getScaledExchangeRate(
      Denomination.builder().assetCode("SRC").assetScale((short) 6).build(),
      Denomination.builder().assetCode("DST").assetScale((short) 9).build(),
      Slippage.NONE
    );
    assertThat(actual.value()).isEqualTo(Ratio.from(BigDecimal.valueOf(1000)));

    actual = oracleExchangeRateService.getScaledExchangeRate(
      Denomination.builder().assetCode("SRC").assetScale((short) 6).build(),
      Denomination.builder().assetCode("DST").assetScale((short) 9).build(),
      Slippage.ONE_PERCENT
    );
    assertThat(actual.value()).isEqualTo(Ratio.from(new BigDecimal(990)));
  }

  @Test
  public void testSourceEqualsDest() {
    when(exchangeRateMock.getFactor()).thenReturn(DefaultNumberValue.of(new BigDecimal("1.0")));
    when(exchangeRateProvider.getExchangeRate(anyString(), anyString())).thenReturn(exchangeRateMock);

    ScaledExchangeRate actual = oracleExchangeRateService.getScaledExchangeRate(
      Denomination.builder().assetCode("SRC").assetScale((short) 6).build(),
      Denomination.builder().assetCode("DST").assetScale((short) 6).build(),
      Slippage.NONE
    );
    assertThat(actual.value()).isEqualTo(Ratio.from(BigDecimal.ONE));

    actual = oracleExchangeRateService.getScaledExchangeRate(
      Denomination.builder().assetCode("SRC").assetScale((short) 6).build(),
      Denomination.builder().assetCode("DST").assetScale((short) 6).build(),
      Slippage.ONE_PERCENT
    );
    assertThat(actual.value()).isEqualTo(Ratio.from(BigDecimal.valueOf(0.99)));
  }

  @Test
  public void testSourceIsZeroAndDestIsZero() {
    when(exchangeRateMock.getFactor()).thenReturn(DefaultNumberValue.of(new BigDecimal("1.0")));
    when(exchangeRateProvider.getExchangeRate(anyString(), anyString())).thenReturn(exchangeRateMock);

    ScaledExchangeRate actual = oracleExchangeRateService.getScaledExchangeRate(
      Denomination.builder().assetCode("SRC").assetScale((short) 0).build(),
      Denomination.builder().assetCode("DST").assetScale((short) 0).build(),
      Slippage.NONE
    );
    assertThat(actual.value()).isEqualByComparingTo(Ratio.ONE);

    actual = oracleExchangeRateService.getScaledExchangeRate(
      Denomination.builder().assetCode("SRC").assetScale((short) 0).build(),
      Denomination.builder().assetCode("DST").assetScale((short) 0).build(),
      Slippage.ONE_PERCENT
    );
    assertThat(actual.value()).isEqualTo(Ratio.from(new BigDecimal("0.99")));
  }

  @Test
  public void testSourceIsZeroAndDestIsPositive() {
    when(exchangeRateMock.getFactor()).thenReturn(DefaultNumberValue.of(new BigDecimal("2.0")));
    when(exchangeRateProvider.getExchangeRate(anyString(), anyString())).thenReturn(exchangeRateMock);

    ScaledExchangeRate actual = oracleExchangeRateService.getScaledExchangeRate(
      Denomination.builder().assetCode("SRC").assetScale((short) 0).build(),
      Denomination.builder().assetCode("DST").assetScale((short) 2).build(),
      Slippage.NONE
    );
    assertThat(actual.value()).isEqualTo(Ratio.from(BigDecimal.valueOf(200)));

    actual = oracleExchangeRateService.getScaledExchangeRate(
      Denomination.builder().assetCode("SRC").assetScale((short) 0).build(),
      Denomination.builder().assetCode("DST").assetScale((short) 2).build(),
      Slippage.ONE_PERCENT
    );
    assertThat(actual.value()).isEqualTo(Ratio.from(new BigDecimal(198)));
  }

  @Test
  public void testSourceIsPositiveAndDestIsZero() {
    when(exchangeRateMock.getFactor()).thenReturn(DefaultNumberValue.of(new BigDecimal("2.0")));
    when(exchangeRateProvider.getExchangeRate(anyString(), anyString())).thenReturn(exchangeRateMock);

    ScaledExchangeRate actual = oracleExchangeRateService.getScaledExchangeRate(
      Denomination.builder().assetCode("SRC").assetScale((short) 2).build(),
      Denomination.builder().assetCode("DST").assetScale((short) 0).build(),
      Slippage.NONE
    );
    assertThat(actual.value()).isEqualTo(Ratio.from(new BigDecimal("0.020")));

    actual = oracleExchangeRateService.getScaledExchangeRate(
      Denomination.builder().assetCode("SRC").assetScale((short) 2).build(),
      Denomination.builder().assetCode("DST").assetScale((short) 0).build(),
      Slippage.ONE_PERCENT
    );
    assertThat(actual.value()).isEqualTo(Ratio.from(new BigDecimal("0.0198")));
  }

  @Test
  public void testSourceIsBigAndDestIsZero() {
    when(exchangeRateMock.getFactor()).thenReturn(DefaultNumberValue.of(new BigDecimal("2.0")));
    when(exchangeRateProvider.getExchangeRate(anyString(), anyString())).thenReturn(exchangeRateMock);

    ScaledExchangeRate actual = oracleExchangeRateService.getScaledExchangeRate(
      Denomination.builder().assetCode("SRC").assetScale((short) 18).build(),
      Denomination.builder().assetCode("DST").assetScale((short) 0).build(),
      Slippage.NONE
    );
    assertThat(actual.value()).isEqualTo(Ratio.from(new BigDecimal("0.000000000000000002")));

    actual = oracleExchangeRateService.getScaledExchangeRate(
      Denomination.builder().assetCode("SRC").assetScale((short) 18).build(),
      Denomination.builder().assetCode("DST").assetScale((short) 0).build(),
      Slippage.ONE_PERCENT
    );
    assertThat(actual.value()).isEqualTo(Ratio.from(new BigDecimal("0.00000000000000000198")));
  }

  @Test
  public void testSourceIsZeroAndDestIsBig() {
    when(exchangeRateMock.getFactor()).thenReturn(DefaultNumberValue.of(new BigDecimal("2.0")));
    when(exchangeRateProvider.getExchangeRate(anyString(), anyString())).thenReturn(exchangeRateMock);

    ScaledExchangeRate actual = oracleExchangeRateService.getScaledExchangeRate(
      Denomination.builder().assetCode("SRC").assetScale((short) 0).build(),
      Denomination.builder().assetCode("DST").assetScale((short) 18).build(),
      Slippage.NONE
    );
    assertThat(actual.value()).isEqualTo(Ratio.from(new BigDecimal("2000000000000000000")));

    actual = oracleExchangeRateService.getScaledExchangeRate(
      Denomination.builder().assetCode("SRC").assetScale((short) 0).build(),
      Denomination.builder().assetCode("DST").assetScale((short) 18).build(),
      Slippage.ONE_PERCENT
    );
    assertThat(actual.value()).isEqualTo(Ratio.from(new BigDecimal("1980000000000000000")));
  }
}
