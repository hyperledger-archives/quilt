package org.interledger.fx;

import static org.assertj.core.api.Assertions.assertThat;

import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.money.CurrencyUnit;
import javax.money.Monetary;

/**
 * Unit tests for {@link JavaMoneyUtils}.
 */
public class JavaMoneyUtilsTest {

  private JavaMoneyUtils javaMoneyUtils;

  @Before
  public void setUp() {
    this.javaMoneyUtils = new JavaMoneyUtils();
  }

  ///////////////////
  // toMonetaryAmount
  ///////////////////

  @Test
  public void toMonetaryAmountInvalidUnits() {
    final CurrencyUnit currencyUSD = Monetary.getCurrency("USD");
    final int assetScale = 2;

    BigDecimal cents = BigDecimal.valueOf(0.1);
    assertThat(
        javaMoneyUtils.toMonetaryAmount(currencyUSD, cents.toBigInteger(), assetScale).getNumber().intValueExact())
        .isEqualTo(0);
  }

  @Test
  public void toMonetaryAmountUSDToUSD() {
    final CurrencyUnit currencyUSD = Monetary.getCurrency("USD");
    final int assetScale = 0;

    BigInteger cents = BigInteger.ZERO;
    assertThat(javaMoneyUtils.toMonetaryAmount(currencyUSD, cents, assetScale)
        .getNumber().numberValue(BigDecimal.class)).isEqualTo(new BigDecimal("0"));

    cents = BigInteger.ONE;
    assertThat(javaMoneyUtils.toMonetaryAmount(currencyUSD, cents, assetScale)
        .getNumber().numberValue(BigDecimal.class)).isEqualTo(new BigDecimal("1"));

    cents = BigInteger.valueOf(100);
    assertThat(javaMoneyUtils.toMonetaryAmount(currencyUSD, cents, assetScale)
        .getNumber().numberValue(BigDecimal.class)).isEqualTo(new BigDecimal("100"));

    cents = BigInteger.valueOf(199);
    assertThat(javaMoneyUtils.toMonetaryAmount(currencyUSD, cents, assetScale)
        .getNumber().numberValue(BigDecimal.class)).isEqualTo(new BigDecimal("199"));

    cents = BigInteger.valueOf(1999);
    assertThat(javaMoneyUtils.toMonetaryAmount(currencyUSD, cents, assetScale)
        .getNumber().numberValue(BigDecimal.class)).isEqualTo(new BigDecimal("1999"));
  }

  @Test
  public void toMonetaryAmountCentsToUSD() {
    final CurrencyUnit currencyUSD = Monetary.getCurrency("USD");
    final int assetScale = 2;

    BigInteger cents = BigInteger.ZERO;
    assertThat(javaMoneyUtils.toMonetaryAmount(currencyUSD, cents, assetScale)
        .getNumber().numberValue(BigDecimal.class)).isEqualTo(new BigDecimal("0"));

    cents = BigInteger.ONE;
    assertThat(javaMoneyUtils.toMonetaryAmount(currencyUSD, cents, assetScale)
        .getNumber().numberValue(BigDecimal.class)).isEqualTo(new BigDecimal("0.01"));

    cents = BigInteger.valueOf(100);
    assertThat(javaMoneyUtils.toMonetaryAmount(currencyUSD, cents, assetScale)
        .getNumber().numberValue(BigDecimal.class)).isEqualTo(new BigDecimal("1"));

    cents = BigInteger.valueOf(199);
    assertThat(javaMoneyUtils.toMonetaryAmount(currencyUSD, cents, assetScale)
        .getNumber().numberValue(BigDecimal.class)).isEqualTo(new BigDecimal("1.99"));

    cents = BigInteger.valueOf(1999);
    assertThat(javaMoneyUtils.toMonetaryAmount(currencyUSD, cents, assetScale)
        .getNumber().numberValue(BigDecimal.class)).isEqualTo(new BigDecimal("19.99"));

  }

  @Test
  public void toMonetaryAmountNanoDollarsToUSD() {
    final CurrencyUnit currencyUSD = Monetary.getCurrency("USD");
    final int assetScale = 9;

    BigInteger cents = BigInteger.ZERO;
    assertThat(javaMoneyUtils.toMonetaryAmount(currencyUSD, cents, assetScale)
        .getNumber().numberValue(BigDecimal.class)).isEqualTo(new BigDecimal("0"));

    cents = BigInteger.ONE;
    assertThat(javaMoneyUtils.toMonetaryAmount(currencyUSD, cents, assetScale)
        .getNumber().numberValue(BigDecimal.class)).isEqualTo(new BigDecimal("0.000000001"));

    cents = BigInteger.valueOf(100);
    assertThat(javaMoneyUtils.toMonetaryAmount(currencyUSD, cents, assetScale)
        .getNumber().numberValue(BigDecimal.class)).isEqualTo(new BigDecimal("0.0000001"));

    cents = BigInteger.valueOf(199);
    assertThat(javaMoneyUtils.toMonetaryAmount(currencyUSD, cents, assetScale)
        .getNumber().numberValue(BigDecimal.class)).isEqualTo(new BigDecimal("0.000000199"));

    cents = BigInteger.valueOf(1999);
    assertThat(javaMoneyUtils.toMonetaryAmount(currencyUSD, cents, assetScale)
        .getNumber().numberValue(BigDecimal.class)).isEqualTo(new BigDecimal("0.000001999"));

  }

  @Test
  public void toMonetaryAmountDropsToXRP() {
    final CurrencyUnit currencyXRP = Monetary.getCurrency("XRP");
    final int assetScale = 6;

    BigInteger cents = BigInteger.ZERO;
    assertThat(javaMoneyUtils.toMonetaryAmount(currencyXRP, cents, assetScale)
        .getNumber().numberValue(BigDecimal.class)).isEqualTo(new BigDecimal("0"));

    cents = BigInteger.ONE;
    assertThat(javaMoneyUtils.toMonetaryAmount(currencyXRP, cents, assetScale)
        .getNumber().numberValue(BigDecimal.class)).isEqualTo(new BigDecimal("0.000001"));

    cents = BigInteger.valueOf(100);
    assertThat(javaMoneyUtils.toMonetaryAmount(currencyXRP, cents, assetScale)
        .getNumber().numberValue(BigDecimal.class)).isEqualTo(new BigDecimal("0.0001"));

    cents = BigInteger.valueOf(199);
    assertThat(javaMoneyUtils.toMonetaryAmount(currencyXRP, cents, assetScale)
        .getNumber().numberValue(BigDecimal.class)).isEqualTo(new BigDecimal("0.000199"));

    cents = BigInteger.valueOf(1999);
    assertThat(javaMoneyUtils.toMonetaryAmount(currencyXRP, cents, assetScale)
        .getNumber().numberValue(BigDecimal.class)).isEqualTo(new BigDecimal("0.001999"));
  }

  /**
   * This test validates that converting a number into a Money that exceeds the default rounding provider's scale occurs
   * correctly.
   */
  @Test
  public void toMonetaryAmountDropsToDrops() {
    final CurrencyUnit currencyXRP = Monetary.getCurrency("XRP");
    final int assetScale = 9;

    BigInteger drops = BigInteger.ZERO;
    assertThat(javaMoneyUtils.toMonetaryAmount(currencyXRP, drops, assetScale)
        .getNumber().numberValue(BigDecimal.class)).isEqualTo(new BigDecimal("0"));

    drops = BigInteger.ONE;
    assertThat(javaMoneyUtils.toMonetaryAmount(currencyXRP, drops, assetScale)
        .getNumber().numberValue(BigDecimal.class)).isEqualTo(new BigDecimal("0.000000001"));

    drops = BigInteger.valueOf(100);
    assertThat(javaMoneyUtils.toMonetaryAmount(currencyXRP, drops, assetScale)
        .getNumber().numberValue(BigDecimal.class)).isEqualTo(new BigDecimal("0.0000001"));

    drops = BigInteger.valueOf(199);
    assertThat(javaMoneyUtils.toMonetaryAmount(currencyXRP, drops, assetScale)
        .getNumber().numberValue(BigDecimal.class)).isEqualTo(new BigDecimal("0.000000199"));

    drops = BigInteger.valueOf(1999);
    assertThat(javaMoneyUtils.toMonetaryAmount(currencyXRP, drops, assetScale)
        .getNumber().numberValue(BigDecimal.class)).isEqualTo(new BigDecimal("0.000001999"));

    drops = BigInteger.valueOf(1000000000L);
    assertThat(javaMoneyUtils.toMonetaryAmount(currencyXRP, drops, assetScale)
        .getNumber().numberValue(BigDecimal.class)).isEqualTo(new BigDecimal("1"));
  }

  @Test
  public void toMonetaryAmountXRPToXRP() {
    final CurrencyUnit currencyXRP = Monetary.getCurrency("XRP");
    final int assetScale = 0;

    BigInteger cents = BigInteger.ZERO;
    assertThat(javaMoneyUtils.toMonetaryAmount(currencyXRP, cents, assetScale)
        .getNumber().numberValue(BigDecimal.class)).isEqualTo(new BigDecimal("0"));

    cents = BigInteger.ONE;
    assertThat(javaMoneyUtils.toMonetaryAmount(currencyXRP, cents, assetScale)
        .getNumber().numberValue(BigDecimal.class)).isEqualTo(new BigDecimal("1"));

    cents = BigInteger.valueOf(100);
    assertThat(javaMoneyUtils.toMonetaryAmount(currencyXRP, cents, assetScale)
        .getNumber().numberValue(BigDecimal.class)).isEqualTo(new BigDecimal("100"));

    cents = BigInteger.valueOf(199);
    assertThat(javaMoneyUtils.toMonetaryAmount(currencyXRP, cents, assetScale)
        .getNumber().numberValue(BigDecimal.class)).isEqualTo(new BigDecimal("199"));

    cents = BigInteger.valueOf(1999);
    assertThat(javaMoneyUtils.toMonetaryAmount(currencyXRP, cents, assetScale)
        .getNumber().numberValue(BigDecimal.class)).isEqualTo(new BigDecimal("1999"));

  }

  ///////////////////
  // toInterledgerAmount
  ///////////////////

  @Test
  public void toInterledgerAmountUSDToUSD() {
    final CurrencyUnit currencyUSD = Monetary.getCurrency("USD");
    final int assetScale = 0;

    Money money = Money.of(BigInteger.ZERO, currencyUSD);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.ZERO);

    money = Money.of(BigInteger.ONE, currencyUSD);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.ONE);

    money = Money.of(BigInteger.valueOf(100), currencyUSD);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.valueOf(100L));

    money = Money.of(BigInteger.valueOf(199), currencyUSD);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.valueOf(199L));

    money = Money.of(BigInteger.valueOf(1999), currencyUSD);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.valueOf(1999L));

    money = Money.of(BigInteger.valueOf(600000000), currencyUSD);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.valueOf(600000000L));
  }

  /**
   * This test validates that scaling a number beyond its default JavaMoney scale occurs correctly. E.g., this test uses
   * XRP, which has a default rounding of 6 digits, whereas this test validates that simple ILP scaling doesn't engage
   * this rounding provider.
   */
  @Test
  public void toInterledgerAmountDropsToDrops() {
    final CurrencyUnit currencyXRP = Monetary.getCurrency("XRP");
    final int assetScale = 9;

    Money money = Money.of(BigInteger.ZERO, currencyXRP);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.ZERO);

    money = Money.of(BigInteger.valueOf(1), currencyXRP);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.valueOf(1000000000L));

    money = Money.of(BigDecimal.valueOf(0.1), currencyXRP);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.valueOf(100000000L));

    money = Money.of(BigDecimal.valueOf(0.01), currencyXRP);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.valueOf(10000000L));

    money = Money.of(BigDecimal.valueOf(0.001), currencyXRP);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.valueOf(1000000L));

    money = Money.of(BigDecimal.valueOf(0.000001), currencyXRP);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.valueOf(1000L));

    money = Money.of(BigDecimal.valueOf(0.000000001), currencyXRP);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.valueOf(1L));

    money = Money.of(BigDecimal.valueOf(0.000000000001), currencyXRP);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.valueOf(0));
  }

  @Test
  public void toInterledgerAmountCentsToUSD() {
    final CurrencyUnit currencyUSD = Monetary.getCurrency("USD");
    final int assetScale = 2;

    // There was an error where 1 drop was being sent to the ping account, which translated the value in
    // `toInterledgerAmount` into cents. The amount in cents was `0.00000027214`, but when translated into cents, this
    // produced an ArithmaticException because the code was using .toBigIntegerExact in an effort to catch rounding
    // errors. However, that code has been changed to instead always round down in instances of precision loss so
    // that rounding errors don't accrue into some large value.
    Money money = Money.of(new BigDecimal("0.0"), currencyUSD);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.ZERO);

    money = Money.of(new BigDecimal("0.00000027214"), currencyUSD);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.ZERO);

    money = Money.of(new BigDecimal("0.001"), currencyUSD);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.ZERO);

    money = Money.of(new BigDecimal("0.009"), currencyUSD);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.ZERO);

    money = Money.of(new BigDecimal("0.01"), currencyUSD);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.ONE);

    money = Money.of(new BigDecimal("1.00"), currencyUSD);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.valueOf(100L));

    money = Money.of(new BigDecimal("1.99"), currencyUSD);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.valueOf(199L));

    money = Money.of(new BigDecimal("19.99"), currencyUSD);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.valueOf(1999L));

    money = Money.of(new BigDecimal("6000000"), currencyUSD);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.valueOf(600000000L));
  }

  @Test
  public void toInterledgerNanoDollarsToUSD() {
    final CurrencyUnit currencyUSD = Monetary.getCurrency("USD");
    final int assetScale = 9;

    Money money = Money.of(new BigDecimal("0.000000000"), currencyUSD);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.ZERO);

    money = Money.of(new BigDecimal("0.00000000099"), currencyUSD);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.ZERO);

    money = Money.of(new BigDecimal("0.000000001"), currencyUSD);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.ONE);

    money = Money.of(new BigDecimal("0.0000001"), currencyUSD);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.valueOf(100L));

    money = Money.of(new BigDecimal("0.000000199"), currencyUSD);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.valueOf(199L));

    money = Money.of(new BigDecimal("0.000001999"), currencyUSD);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.valueOf(1999L));

    money = Money.of(new BigDecimal("6000000"), currencyUSD);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.valueOf(6000000000000000L));
  }

  @Test
  public void toInterledgerAmountDropsToXRP() {

    final CurrencyUnit currencyUSD = Monetary.getCurrency("XRP");
    final int assetScale = 6;

    Money money = Money.of(new BigDecimal("0.00000000"), currencyUSD);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.ZERO);

    money = Money.of(new BigDecimal("0.000000001"), currencyUSD);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.ZERO);

    money = Money.of(new BigDecimal("0.00000001"), currencyUSD);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.ZERO);

    money = Money.of(new BigDecimal("0.000001"), currencyUSD);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.ONE);

    money = Money.of(new BigDecimal("0.0001"), currencyUSD);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.valueOf(100L));

    money = Money.of(new BigDecimal("0.000199"), currencyUSD);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.valueOf(199L));

    money = Money.of(new BigDecimal("0.001999"), currencyUSD);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.valueOf(1999L));

    money = Money.of(new BigDecimal("6000001"), currencyUSD);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.valueOf(6000001000000L));
  }

  @Test
  public void toInterledgerAmountXRPToXRP() {

    final CurrencyUnit currencyUSD = Monetary.getCurrency("XRP");
    final int assetScale = 0;

    Money money = Money.of(BigInteger.ONE, currencyUSD);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.ONE);

    money = Money.of(BigInteger.valueOf(100), currencyUSD);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.valueOf(100L));

    money = Money.of(BigInteger.valueOf(199), currencyUSD);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.valueOf(199L));

    money = Money.of(BigInteger.valueOf(1999), currencyUSD);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.valueOf(1999L));

    money = Money.of(BigInteger.valueOf(600000000), currencyUSD);
    assertThat(javaMoneyUtils.toInterledgerAmount(money, assetScale)).isEqualTo(BigInteger.valueOf(600000000L));
  }
}
