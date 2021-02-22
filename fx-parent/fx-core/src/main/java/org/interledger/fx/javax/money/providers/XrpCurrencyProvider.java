package org.interledger.fx.javax.money.providers;

import com.google.common.collect.ImmutableSet;
import org.javamoney.moneta.CurrencyUnitBuilder;

import java.util.Collections;
import java.util.Set;
import javax.money.CurrencyQuery;
import javax.money.CurrencyUnit;
import javax.money.spi.CurrencyProviderSpi;

/**
 * An implementation of {@link CurrencyProviderSpi} for registering XRP.
 */
public class XrpCurrencyProvider implements CurrencyProviderSpi {

  public static final String DROP = "DROP";
  public static final String XRP = "XRP";

  private final Set<CurrencyUnit> currencyUnits;

  /**
   * No-args Constructor.
   */
  public XrpCurrencyProvider() {
    this.currencyUnits = ImmutableSet.<CurrencyUnit>builder()
      .add(
        CurrencyUnitBuilder.of(XRP, "XrpCurrencyProvider")
          // XRP is generally modelled in the thousandths (but rounding is to the millionth)
          .setDefaultFractionDigits(3)
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
    // only ensure DRT is the code, or it is a default query.
    if (query.isEmpty() || query.getCurrencyCodes().contains(XRP)) {
      return currencyUnits;
    }
    return Collections.emptySet();
  }
}
