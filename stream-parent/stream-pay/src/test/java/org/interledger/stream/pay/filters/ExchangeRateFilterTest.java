package org.interledger.stream.pay.filters;

import org.interledger.stream.pay.trackers.ExchangeRateTracker;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link ExchangeRateFilter}.
 */
public class ExchangeRateFilterTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  ExchangeRateTracker exchangeRateTracker;

  private ExchangeRateFilter exchangeRateFilter;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    this.exchangeRateFilter = new ExchangeRateFilter(exchangeRateTracker);
  }

  @Test
  public void constructWithNull() {
    expectedException.expect(NullPointerException.class);
    new ExchangeRateFilter(null);
  }

  ////////////
  // nextState
  ////////////

  // TODO: Finish these tests!

  @Test
  public void nextState() {
  }

  ////////////
  // doFilter
  ////////////

  @Test
  public void doFilter() {
  }
}