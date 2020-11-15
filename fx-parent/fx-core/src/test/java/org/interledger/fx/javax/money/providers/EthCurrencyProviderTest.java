package org.interledger.fx.javax.money.providers;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import javax.money.CurrencyQueryBuilder;
import javax.money.CurrencyUnit;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link EthCurrencyProvider }.
 */
public class EthCurrencyProviderTest {

  private EthCurrencyProvider provider;

  @Before
  public void setUp() {
    provider = new EthCurrencyProvider();
  }

  @Test
  public void getCurrencies() {
    final Set<CurrencyUnit> currencyUnits = provider.getCurrencies(
      CurrencyQueryBuilder.of()
        .setCurrencyCodes(EthCurrencyProvider.ETH)
        .setCountries(Locale.CANADA) // Land
        .set(LocalDate.of(1970, 1, 1)) // Datum
        .setProviderNames(EthCurrencyProvider.ETH).build() // Provider
    );

    assertThat(currencyUnits.size()).isEqualTo(1);
    assertThat(currencyUnits.stream().findFirst().get().getCurrencyCode()).isEqualTo(EthCurrencyProvider.ETH);
  }

}
