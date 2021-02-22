package org.interledger.fx;

/**
 * Utility instances of various commonly used denominations.
 */
public final class Denominations {

  public static final Denomination XRP_DROPS = Denomination.builder()
    .assetCode("XRP")
    .assetScale((short) 6)
    .build();

  public static final Denomination XRP_MILLI_DROPS = Denomination.builder()
    .assetCode("XRP")
    .assetScale((short) 9)
    .build();

  public static final Denomination USD = Denomination.builder()
    .assetCode("USD")
    .assetScale((short) 0)
    .build();

  public static final Denomination USD_CENTS = Denomination.builder()
    .assetCode("USD")
    .assetScale((short) 2)
    .build();

  public static final Denomination USD_MILLI_DOLLARS = Denomination.builder()
    .assetCode("USD")
    .assetScale((short) 6)
    .build();

  public static final Denomination EUR = Denomination.builder()
    .assetCode("EUR")
    .assetScale((short) 0)
    .build();

  public static final Denomination EUR_CENTS = Denomination.builder()
    .assetCode("EUR")
    .assetScale((short) 2)
    .build();

  private Denominations() {
  }
}
