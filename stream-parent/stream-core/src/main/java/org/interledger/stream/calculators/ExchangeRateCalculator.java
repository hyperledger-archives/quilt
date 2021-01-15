package org.interledger.stream.calculators;

import org.interledger.stream.Denomination;

import com.google.common.primitives.UnsignedLong;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * <p>Defines how a STREAM sender or receiver can calculate the amount to send on a per-packet basis, in addition to
 * acceptable amounts the receiver should accept, based upon implementation-defined market and path exchange-rate
 * mechanisms.</p>
 *
 * <p>For example, an advanced implementation of this interface might decide to compare actual market exchange rates
 * from an Oracle (e.g., the ECB) against the observed path-exchange rates. Using these metrics, the implementation
 * could adjust amounts in an effort to make sure the proper amount of value is traversing the payment path from sender
 * to receiver.</p>
 *
 * @deprecated Will be removed in a future version; prefer ILP Pay instead of Stream Sender functionality.
 */
@Deprecated
public interface ExchangeRateCalculator {

  /**
   * Calculates the amount to send for the ILP packet based on a desired amount to be received by the receiver (in
   * receiver's units).
   *
   * @param amountToReceive      An {@link UnsignedLong} containing the amount to be received (in receiver's units)
   * @param sendDenomination     A {@link Denomination} representing the sender's denomination.
   * @param receiverDenomination A {@link Denomination} representing the receiver's denomination.
   *
   * @return An {@link UnsignedLong} containing amount to send in the ILP packet (in sender's denomination).
   *
   * @throws NoExchangeRateException if exchange rate could not be calculated
   * @deprecated Should be removed or moved if used somewhere in the receiver (not used in sender)
   */
  @Deprecated
  UnsignedLong calculateAmountToSend(
    UnsignedLong amountToReceive, Denomination sendDenomination, Denomination receiverDenomination
  ) throws NoExchangeRateException;

  /**
   * Calculate the minimum amount a receiver should accept based on the exchange rate applied to the {@code
   * sendAmount}.
   *
   * @param sendAmount             An {@link UnsignedLong} representing the amount to send.
   * @param sendAmountDenomination asset code and scale of the amount to send.
   *
   * @return An {@link UnsignedLong} containing the minimum amount the receiver should accept, in sender's units.
   */
  UnsignedLong calculateMinAmountToAccept(UnsignedLong sendAmount, Denomination sendAmountDenomination)
    throws NoExchangeRateException;

  /**
   * Calculate the minimum amount a receiver should accept based on the exchange rate applied to the {@code
   * sendAmount}.
   *
   * @param sendAmount             An {@link UnsignedLong} containing the amount to send.
   * @param sendAmountDenomination A {@link Denomination} for  {@code amountToSend}.
   * @param receiverDenomination   A {@link Denomination} for the receiver.
   *
   * @return An {@link UnsignedLong} containing the minimum amount the receiver should accept, in sender's units.
   */
  default UnsignedLong calculateMinAmountToAccept(
    UnsignedLong sendAmount, Denomination sendAmountDenomination, Denomination receiverDenomination
  ) throws NoExchangeRateException {
    return calculateMinAmountToAccept(sendAmount, sendAmountDenomination);
  }

  /**
   * Convenience method to compute the BigDecimal representation of an amount for a given asset scale.
   *
   * @param amount       the non-decimal version of the amount
   * @param denomination definition of the scale
   *
   * @return the scaled decimal amount
   */
  default BigDecimal scaled(final UnsignedLong amount, final Denomination denomination) {
    Objects.requireNonNull(amount);
    Objects.requireNonNull(denomination);

    BigDecimal scale = BigDecimal.ONE.scaleByPowerOfTen(denomination.assetScale());
    return new BigDecimal(amount.bigIntegerValue()).divide(scale);
  }
}
