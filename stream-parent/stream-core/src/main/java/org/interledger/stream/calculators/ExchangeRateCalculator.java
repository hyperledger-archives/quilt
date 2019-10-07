package org.interledger.stream.calculators;

import com.google.common.primitives.UnsignedLong;
import org.interledger.stream.Denomination;

import java.math.BigDecimal;
import java.util.Optional;

public interface ExchangeRateCalculator {

  /**
   * Calculate the minimum amount a receiver should accept based on the exchange rate applied to the sendAmount.
   * @param sendAmount                   amount to send.
   * @param sendDenomination             asset code and scale of the amount to send.
   * @param expectedReceivedDenomination asset code and scale to apply to the amount the receiver should receive
   *                                     when computing the total for an exchange rate. Optional if denomination of
   *                                     receiver is unknown. ExchangeRateCalculator implementations should throw a
   *                                     {@link NoExchangeRateException}
   * @return                             the minimum amount the receiver should accept.
   * @throws NoExchangeRateException if exchange rate could not be calculated
   */
  UnsignedLong calculate(UnsignedLong sendAmount, Denomination sendDenomination,
                         Optional<Denomination> expectedReceivedDenomination)
    throws NoExchangeRateException;

  /**
   * Convenience method to compute the BigDecimal representation of an amount for a given asset scale
   * @param amount the non-decimal version of the amount
   * @param denomination definition of the scale
   * @return the scaled decimal amount
   */
  default BigDecimal scaled(UnsignedLong amount, Denomination denomination) {
    BigDecimal scale = BigDecimal.ONE.scaleByPowerOfTen(denomination.assetScale());
    return new BigDecimal(amount.bigIntegerValue()).divide(scale);
  }
}
