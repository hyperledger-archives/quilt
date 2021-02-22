package org.interledger.fx.javax.money.providers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import javax.money.Monetary;

public class MonetaryTest {

  @Test
  public void getCurrency() {
    assertThat(Monetary.getCurrency("XRP").getDefaultFractionDigits()).isEqualTo(3);
    assertThat(Monetary.getCurrency("ETH").getDefaultFractionDigits()).isEqualTo(9);
    assertThat(Monetary.getCurrency("USD").getDefaultFractionDigits()).isEqualTo(2);
    assertThat(Monetary.getCurrency("EUR").getDefaultFractionDigits()).isEqualTo(2);
    assertThat(Monetary.getCurrency("JPY").getDefaultFractionDigits()).isEqualTo(0);
    assertThat(Monetary.getCurrency("MXN").getDefaultFractionDigits()).isEqualTo(2);
    assertThat(Monetary.getCurrency("PHP").getDefaultFractionDigits()).isEqualTo(2);
  }

}
