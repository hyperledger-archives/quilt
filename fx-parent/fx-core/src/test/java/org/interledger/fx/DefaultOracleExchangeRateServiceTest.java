package org.interledger.fx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

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
  public void testZeroAndZero() {
    when(exchangeRateMock.getFactor()).thenReturn(DefaultNumberValue.of(new BigDecimal("1.0")));
    when(exchangeRateProvider.getExchangeRate(anyString(), anyString())).thenReturn(exchangeRateMock);

    BigDecimal actual = oracleExchangeRateService.getScaledExchangeRate(
      Denomination.builder().assetCode("SRC").assetScale((short) 0).build(),
      Denomination.builder().assetCode("DST").assetScale((short) 0).build(),
      Slippage.NONE
    );
    assertThat(actual).isEqualByComparingTo(new BigDecimal("1"));
  }

  @Test
  public void testZeroAndTwo() {
    when(exchangeRateMock.getFactor()).thenReturn(DefaultNumberValue.of(new BigDecimal("2.0")));
    when(exchangeRateProvider.getExchangeRate(anyString(), anyString())).thenReturn(exchangeRateMock);

    BigDecimal actual = oracleExchangeRateService.getScaledExchangeRate(
      Denomination.builder().assetCode("SRC").assetScale((short) 0).build(),
      Denomination.builder().assetCode("DST").assetScale((short) 2).build(),
      Slippage.NONE
    );
    assertThat(actual).isEqualByComparingTo(BigDecimal.valueOf(200));

    actual = oracleExchangeRateService.getScaledExchangeRate(
      Denomination.builder().assetCode("SRC").assetScale((short) 0).build(),
      Denomination.builder().assetCode("DST").assetScale((short) 2).build(),
      Slippage.ONE_PERCENT
    );
    assertThat(actual).isEqualByComparingTo(new BigDecimal(198));

  }

  @Test
  public void testTwoAndZero() {
    when(exchangeRateMock.getFactor()).thenReturn(DefaultNumberValue.of(new BigDecimal("2.0")));
    when(exchangeRateProvider.getExchangeRate(anyString(), anyString())).thenReturn(exchangeRateMock);

    BigDecimal actual = oracleExchangeRateService.getScaledExchangeRate(
      Denomination.builder().assetCode("SRC").assetScale((short) 2).build(),
      Denomination.builder().assetCode("DST").assetScale((short) 0).build(),
      Slippage.NONE
    );
    assertThat(actual).isEqualByComparingTo(new BigDecimal("0.020"));

    actual = oracleExchangeRateService.getScaledExchangeRate(
      Denomination.builder().assetCode("SRC").assetScale((short) 2).build(),
      Denomination.builder().assetCode("DST").assetScale((short) 0).build(),
      Slippage.ONE_PERCENT
    );
    assertThat(actual).isEqualByComparingTo(new BigDecimal("0.0198"));
  }

  @Test
  public void test18AndZero() {
    when(exchangeRateMock.getFactor()).thenReturn(DefaultNumberValue.of(new BigDecimal("2.0")));
    when(exchangeRateProvider.getExchangeRate(anyString(), anyString())).thenReturn(exchangeRateMock);

    BigDecimal actual = oracleExchangeRateService.getScaledExchangeRate(
      Denomination.builder().assetCode("SRC").assetScale((short) 18).build(),
      Denomination.builder().assetCode("DST").assetScale((short) 0).build(),
      Slippage.NONE
    );
    assertThat(actual).isEqualByComparingTo(new BigDecimal("0.000000000000000002"));

    actual = oracleExchangeRateService.getScaledExchangeRate(
      Denomination.builder().assetCode("SRC").assetScale((short) 18).build(),
      Denomination.builder().assetCode("DST").assetScale((short) 0).build(),
      Slippage.ONE_PERCENT
    );
    assertThat(actual).isEqualByComparingTo(new BigDecimal("0.00000000000000000198"));
  }
}
