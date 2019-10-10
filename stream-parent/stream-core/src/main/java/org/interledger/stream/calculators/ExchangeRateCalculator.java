package org.interledger.stream.calculators;

import org.interledger.stream.Denomination;

import com.google.common.primitives.UnsignedLong;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * <p>Defines how a STREAM sender or receiver can calculate the amount to send on a per-packet basis, in addition to
 * acceptable amounts the receiver should accept, based upon implementation-defined market and path exchange-rate
 * mechanisms.</p>
 *
 * <p>For example, an advanced implementation of this interface might decide to compare actual market exchange rates
 * from an Oracle (e.g., the ECB) against the observed path-exchange rates. Using these metrics, the implementation
 * could adjust amounts in an effort to make sure the proper amount of value is traversing the payment path from sender
 * to receiver.</p>
 */
public interface ExchangeRateCalculator {

  /**
   * Calculates the amount to send for the ILP packet based on a desired amount to be received by the receiver (in
   * receiver's units).
   *
   * @param amountToReceive     amount to be received (in receiver's units)
   * @param sendDenomination    sender's denomination
   * @param receiveDenomination receiver's denomination
   *
   * @return amount to send in the ILP packet (in sender's denomination)
   *
   * @throws NoExchangeRateException if exchange rate could not be calculated
   */
  UnsignedLong calculateAmountToSend(UnsignedLong amountToReceive,
      Denomination sendDenomination,
      Denomination receiveDenomination)
      throws NoExchangeRateException;

  /**
   * Calculate the minimum amount a receiver should accept based on the exchange rate applied to the sendAmount.
   *
   * @param sendAmount                   amount to send.
   * @param sendDenomination             asset code and scale of the amount to send.
   * @param expectedReceivedDenomination asset code and scale to apply to the amount the receiver should receive when
   *                                     computing the total for an exchange rate. Optional if denomination of receiver
   *                                     is unknown. ExchangeRateCalculator implementations should throw a {@link
   *                                     NoExchangeRateException}
   *
   * @return the minimum amount the receiver should accept.
   *
   * @throws NoExchangeRateException if exchange rate could not be calculated
   */
  UnsignedLong calculateMinAmountToAccept(UnsignedLong sendAmount, Denomination sendDenomination,
      Optional<Denomination> expectedReceivedDenomination)
      throws NoExchangeRateException;

  /**
   * Convenience method to compute the BigDecimal representation of an amount for a given asset scale.
   *
   * @param amount       the non-decimal version of the amount
   * @param denomination definition of the scale
   *
   * @return the scaled decimal amount
   */
  default BigDecimal scaled(UnsignedLong amount, Denomination denomination) {
    BigDecimal scale = BigDecimal.ONE.scaleByPowerOfTen(denomination.assetScale());
    return new BigDecimal(amount.bigIntegerValue()).divide(scale);
  }
}
