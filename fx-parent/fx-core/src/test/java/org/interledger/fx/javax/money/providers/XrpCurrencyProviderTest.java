package org.interledger.fx.javax.money.providers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.fx.javax.money.providers.XrpCurrencyProvider.DROP;
import static org.interledger.fx.javax.money.providers.XrpCurrencyProvider.XRP;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import javax.money.CurrencyQueryBuilder;
import javax.money.CurrencyUnit;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link XrpCurrencyProvider}.
 */
public class XrpCurrencyProviderTest {

  private XrpCurrencyProvider provider;

  @Before
  public void setUp() {
    provider = new XrpCurrencyProvider();
  }

  @Test
  public void getCurrencies() {
    final Set<CurrencyUnit> currencyUnits = provider.getCurrencies(
      CurrencyQueryBuilder.of()
        .setCurrencyCodes(XRP)
        .setCountries(Locale.CANADA) // Land
        .set(LocalDate.of(1970, 1, 1)) // Datum
        .setProviderNames(XRP).build() // Provider
    );

    assertThat(currencyUnits.size()).isEqualTo(1);
    assertThat(currencyUnits.stream().findFirst().get().getCurrencyCode()).isEqualTo(XRP);
  }

  @Test
  public void getCurrenciesUnknownProvider() {
    final Set<CurrencyUnit> currencyUnits = provider.getCurrencies(
      CurrencyQueryBuilder.of()
        .setCountries(Locale.CANADA) // Land
        .set(LocalDate.of(1970, 1, 1)) // Datum
        .setProviderNames(DROP).build() // Provider
    );

    assertThat(currencyUnits.size()).isEqualTo(0);
  }
}
