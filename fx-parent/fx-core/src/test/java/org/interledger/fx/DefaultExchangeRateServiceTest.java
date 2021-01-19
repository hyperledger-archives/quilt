package org.interledger.fx;

import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.money.convert.ExchangeRateProvider;

/**
 * Unit tests for {@link DefaultExchangeRateService}.
 */
public class DefaultExchangeRateServiceTest {

  @Mock
  private ExchangeRateProvider exchangeRateProvider;

  private ExchangeRateService exchangeRateService;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    exchangeRateService = new DefaultExchangeRateService(exchangeRateProvider);
  }

  // See Parameterized Test for how to do FX.
}
