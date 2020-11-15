package org.interledger.fx.javax.money.providers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.fx.javax.money.providers.XrpCurrencyProvider.DROP;
import static org.interledger.fx.javax.money.providers.XrpCurrencyProvider.XRP;

import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import javax.money.Monetary;
import javax.money.MonetaryAmount;
import javax.money.MonetaryRounding;
import javax.money.RoundingQueryBuilder;
import javax.money.UnknownCurrencyException;

/**
 * Unit tests for {@link DropRoundingProvider}. This test validates that whatever precision a
 * particular XRP value is in, it will always be rounded to the millionth of an XRP, or 1 DROP,
 * which is the smallest unit that XRP can handle.
 */
public class DropRoundingProviderTest {

  private DropRoundingProvider dropRoundingProvider;

  @Before
  public void setUp() {
    dropRoundingProvider = new DropRoundingProvider();
  }

  @Test
  public void testRoundDropsWithAllDecimals() {
    final MonetaryAmount xrpAmount = Monetary.getAmountFactory(Money.class)
      .setCurrency(XRP)
      .setNumber(new BigDecimal("0.123456789"))
      .create();
    assertThat(xrpAmount.getNumber().numberValue(BigDecimal.class))
      .isEqualTo(new BigDecimal("0.123456789"));

    // round it to Drops
    final MonetaryAmount xrpRounded = xrpAmount.with(Monetary.getRounding(xrpAmount.getCurrency()));
    assertThat(xrpRounded.getNumber().numberValue(BigDecimal.class))
      .isEqualTo(new BigDecimal("0.123457"));
  }

  /**
   * This test ensures that when very small amounts of XRP are rounded, the rounded amount does not
   * exceed 1 drop, or one-millionth of an XRP.
   */
  @Test
  public void testRoundXrpWhenSmallerThanDrops() {
    final MonetaryAmount xrpAmount = Monetary.getAmountFactory(Money.class)
      .setCurrency(XRP)
      .setNumber(new BigDecimal("0.000000001"))
      .create();
    assertThat(xrpAmount.getNumber().numberValue(BigDecimal.class))
      .isEqualTo(new BigDecimal("0.000000001"));

    // round it to Drops
    final MonetaryAmount xrpRounded = xrpAmount.with(Monetary.getRounding(xrpAmount.getCurrency()));
    assertThat(xrpRounded.getNumber().numberValue(BigDecimal.class)).isEqualTo(new BigDecimal("0"));
  }

  @Test
  public void testRoundDropsWith1Xrp() {
    final Money xrpAmount = Monetary.getAmountFactory(Money.class)
      .setCurrency(XRP)
      .setNumber(new BigDecimal("1.123456789"))
      .create();
    assertThat(xrpAmount.getNumber().numberValue(BigDecimal.class))
      .isEqualTo(new BigDecimal("1.123456789"));

    // round it to Drops
    final MonetaryAmount xrpRounded = xrpAmount.with(Monetary.getRounding(xrpAmount.getCurrency()));
    assertThat(xrpRounded.getNumber().numberValue(BigDecimal.class))
      .isEqualTo(new BigDecimal("1.123457"));
  }

  @Test
  public void testRoundDrops() {
    final MonetaryAmount xrpAmount = Monetary.getAmountFactory(Money.class)
      .setCurrency(XRP)
      .setNumber(new BigDecimal("221.123456"))
      .create();
    assertThat(xrpAmount.getNumber().numberValue(BigDecimal.class))
      .isEqualTo(new BigDecimal("221.123456"));

    // round it to Drops
    final MonetaryAmount xrpRounded = xrpAmount.with(Monetary.getRounding(xrpAmount.getCurrency()));
    assertThat(xrpRounded.getNumber().numberValue(BigDecimal.class))
      .isEqualTo(new BigDecimal("221.123456"));
  }

  @Test
  public void getRounding() {
    final MonetaryRounding rounding = dropRoundingProvider.getRounding(RoundingQueryBuilder.of()
      .setCurrency(Monetary.getCurrency(XRP))
      .build());
    assertThat(rounding.getRoundingContext().getProviderName()).isEqualTo("DropsProvider");
    assertThat(rounding.getRoundingContext().getRoundingName()).isEqualTo(DROP);
  }

  @Test(expected = UnknownCurrencyException.class)
  public void getRoundingNotFound() {
    try {
      dropRoundingProvider.getRounding(RoundingQueryBuilder.of()
        .setCurrency(Monetary.getCurrency("Foo"))
        .build());
    } catch (UnknownCurrencyException e) {
      assertThat(e.getMessage()).isEqualTo("Unknown currency code: Foo");
      throw e;
    }
  }

  @Test
  public void getRoundingNames() {
    assertThat(dropRoundingProvider.getRoundingNames().size()).isEqualTo(1);
    assertThat(dropRoundingProvider.getRoundingNames().contains(DROP)).isTrue();
  }
}
