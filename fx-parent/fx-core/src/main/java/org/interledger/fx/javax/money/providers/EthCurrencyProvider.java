package org.interledger.fx.javax.money.providers;

import com.google.common.collect.ImmutableSet;
import org.javamoney.moneta.CurrencyUnitBuilder;

import java.util.Collections;
import java.util.Set;
import javax.money.CurrencyQuery;
import javax.money.CurrencyUnit;
import javax.money.spi.CurrencyProviderSpi;

/**
 * An implementation of {@link CurrencyProviderSpi} for registering ETH.
 */
public class EthCurrencyProvider implements CurrencyProviderSpi {

  public static final String ETH = "ETH";

  private Set<CurrencyUnit> currencyUnits;

  /**
   * No-args Constructor.
   */
  public EthCurrencyProvider() {
    this.currencyUnits = ImmutableSet.<CurrencyUnit>builder()
      .add(
        CurrencyUnitBuilder.of(ETH, "EthCurrencyProvider")
          .setDefaultFractionDigits(9) // 1 wei
          .build()
      )
      .build();
  }

  /**
   * Return a {@link CurrencyUnit} instances matching the given {@link CurrencyQuery}.
   *
   * @param query the {@link CurrencyQuery} containing the parameters determining the query. not null.
   *
   * @return the corresponding {@link CurrencyUnit}s matching, never null.
   */
  @Override
  public Set<CurrencyUnit> getCurrencies(CurrencyQuery query) {
    if (query.isEmpty() || query.getCurrencyCodes().contains(ETH)) {
      return currencyUnits;
    }
    return Collections.emptySet();
  }
}
