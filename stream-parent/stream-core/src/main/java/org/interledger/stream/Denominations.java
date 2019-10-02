package org.interledger.stream;

import org.interledger.stream.Denomination;

public final class Denominations {
  private Denominations(){}

  public static final Denomination XRP = Denomination.builder()
      .assetCode("XRP")
      .assetScale((short) 6)
      .build();

  public static final Denomination USD = Denomination.builder()
      .assetCode("USD")
      .assetScale((short) 0)
      .build();

  public static final Denomination US_CENTS = Denomination.builder()
      .assetCode("USD")
      .assetScale((short) 2)
      .build();

  public static final Denomination EUR = Denomination.builder()
      .assetCode("EUR")
      .assetScale((short) 0)
      .build();

  public static final Denomination EUR_CENTS = Denomination.builder()
      .assetCode("EUR")
      .assetScale((short) 2)
      .build();
}
