package org.interledger.fx;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

/**
 * Unit tests for {@link Denominations}.
 */
public class DenominationsTest {

  @Test
  public void values() {

    assertThat(Denominations.XRP_DROPS)
      .extracting("assetCode", "assetScale")
      .containsExactly("XRP", (short) 6);

    assertThat(Denominations.XRP_DROPS)
      .extracting("assetCode", "assetScale")
      .containsExactly("XRP", (short) 6);

    assertThat(Denominations.XRP_MILLI_DROPS)
      .extracting("assetCode", "assetScale")
      .containsExactly("XRP", (short) 9);

    assertThat(Denominations.USD_CENTS)
      .extracting("assetCode", "assetScale")
      .containsExactly("USD", (short) 2);

    assertThat(Denominations.USD_CENTS)
      .extracting("assetCode", "assetScale")
      .containsExactly("USD", (short) 2);

    assertThat(Denominations.USD)
      .extracting("assetCode", "assetScale")
      .containsExactly("USD", (short) 0);

    assertThat(Denominations.EUR_CENTS)
      .extracting("assetCode", "assetScale")
      .containsExactly("EUR", (short) 2);

    assertThat(Denominations.EUR)
      .extracting("assetCode", "assetScale")
      .containsExactly("EUR", (short) 0);
  }

  @Test
  public void equalsHashCode() {
    assertThat(Denominations.EUR).isEqualTo(Denominations.EUR);
    assertThat(Denominations.EUR).isNotEqualTo(Denominations.EUR_CENTS);
    assertThat(Denominations.EUR).isNotEqualTo(Denominations.USD);
    assertThat(Denominations.EUR.hashCode()).isEqualTo(Denominations.EUR.hashCode());
    assertThat(Denominations.EUR.hashCode()).isNotEqualTo(Denominations.EUR_CENTS.hashCode());
    assertThat(Denominations.EUR.hashCode()).isNotEqualTo(Denominations.USD.hashCode());
  }
}
