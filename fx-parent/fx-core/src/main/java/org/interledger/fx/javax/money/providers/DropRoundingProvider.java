package org.interledger.fx.javax.money.providers;

import com.google.common.collect.ImmutableSet;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import javax.money.CurrencyUnit;
import javax.money.MonetaryAmount;
import javax.money.MonetaryRounding;
import javax.money.RoundingContext;
import javax.money.RoundingContextBuilder;
import javax.money.RoundingQuery;
import javax.money.spi.RoundingProviderSpi;

/**
 * An implementation of {@link RoundingProviderSpi} for rounding XRP drops, which are 1 millionth of an XRP.
 *
 * @see "https://developers.ripple.com/currency-formats.html"
 * @see "https://jaxenter.de/go-for-the-money-einfuehrung-in-das-money-and-currency-api-38668"
 */
public class DropRoundingProvider implements RoundingProviderSpi {

  private final Set<String> roundingNames;
  private final MonetaryRounding dropsRounding = new DropsRounding();

  public DropRoundingProvider() {
    this.roundingNames = ImmutableSet.of(XrpCurrencyProvider.DROP);
  }

  /**
   * Get the {@link MonetaryRounding} for this provider.
   *
   * @param query A {@link RoundingQuery}.
   *
   * @return A {@link MonetaryRounding}.
   */
  public MonetaryRounding getRounding(RoundingQuery query) {
    final CurrencyUnit currency = query.getCurrency();
    if (currency != null && (XrpCurrencyProvider.XRP.equals(currency.getCurrencyCode()))) {
      return dropsRounding;
    } else if (XrpCurrencyProvider.DROP.equals(query.getRoundingName())) {
      return dropsRounding;
    }
    return null;

  }

  public Set<String> getRoundingNames() {
    return roundingNames;
  }

  /**
   * A {@link MonetaryRounding} for rounding XRP Drops (1 millionth of an XRP).
   *
   * @see "https://jaxenter.de/go-for-the-money-einfuehrung-in-das-money-and-currency-api-38668"
   */
  public static final class DropsRounding implements MonetaryRounding {

    private final int scale = 6;

    @Override
    public RoundingContext getRoundingContext() {
      return RoundingContextBuilder.of("DropsProvider", XrpCurrencyProvider.DROP).build();
    }

    @Override
    public MonetaryAmount apply(MonetaryAmount amount) {
      // Unlimited Precision, half-up.
      //      final NumberValue value = amount.getNumber().round(new MathContext(
      //        MathContext.DECIMAL128.getPrecision(),
      //        RoundingMode.HALF_UP)
      //      );

      //RoundingQueryBuilder.of().setScale(4).set(RoundingMode.HALF_UP).build();

      return amount.getFactory()
        .setCurrency(amount.getCurrency())
        .setNumber(
          amount.getNumber().numberValue(BigDecimal.class).setScale(this.scale, RoundingMode.HALF_EVEN)
        ).create();

      //final NumberValue value = amount.getNumber().round(new MathContext(6, RoundingMode.HALF_EVEN));
      //return amount.getFactory().setNumber(value).create();
    }
  }
}
