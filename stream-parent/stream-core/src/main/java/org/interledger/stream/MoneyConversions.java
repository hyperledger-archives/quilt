package org.interledger.stream;

import org.interledger.core.fluent.Ratio;

import com.google.common.primitives.UnsignedLong;

import java.util.Objects;

/**
 * Provides utilites for dealing with Exchange Rates.
 */
public class MoneyConversions {

//  // TODO: always ceiling?
//  public static Ratio exchangeRate(final UnsignedLong numerator, final UnsignedLong denoninator) {
//    Objects.requireNonNull(numerator);
//    Objects.requireNonNull(denoninator);
//
//    return Ratio.builder()
//        .numerator(numerator)
//        .denominator(denoninator)
//        .build();
//  }

  /**
   * @deprecated Use FluentUnsignedLong Instead.
   */
  @Deprecated
  public static UnsignedLong multiplyFloor(final UnsignedLong value, final Ratio ratio) {
    Objects.requireNonNull(ratio);

    // TODO: Unit test
    return (value.times(ratio.numerator())).dividedBy(ratio.denominator());
  }

}
