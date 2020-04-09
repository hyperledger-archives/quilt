package org.interledger.stream;

/**
 * Utility instances of various commonly used denominations.
 */
public final class Denominations {

  public static final Denomination XRP_DROPS = Denomination.builder()
      .assetCode("XRP")
      .assetScale((short) 6)
      .build();

  /**
   * @deprecated This value will be removed in a future version because it should actually have a scale of 0. Prefer
   *     {@link #XRP_DROPS} instead.
   */
  @Deprecated
  public static final Denomination XRP = XRP_DROPS;

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

  /**
   * @deprecated This value will be removed in a future version because it is misnamed. Prefer {@link #USD_CENTS}
   *     instead.
   */
  public static final Denomination US_CENTS = USD_CENTS;

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
