package org.interledger.fx;

import javax.money.convert.ExchangeRateProvider;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link DefaultExchangeRateService}.
 */
public class DefaultExchangeRateServiceTest {

  @Mock
  private ExchangeRateProvider exchangeRateProvider;

  private ExchangeRateService exchangeRateService;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    exchangeRateService = new DefaultExchangeRateService(exchangeRateProvider);
  }

  // See Parameterized Test for how to do FX.
}
