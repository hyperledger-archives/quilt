package org.interledger.stream;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class DenominationsTest {

  @Test
  public void values() {
    assertThat(Denominations.XRP)
        .extracting("assetCode", "assetScale")
        .containsExactly("XRP", (short) 6);
    assertThat(Denominations.US_CENTS)
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
}
