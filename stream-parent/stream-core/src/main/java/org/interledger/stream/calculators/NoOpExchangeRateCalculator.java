package org.interledger.stream.calculators;

import org.interledger.stream.Denomination;

import com.google.common.primitives.UnsignedLong;

import java.util.Objects;

/**
 * Exchange rate calculator that ignores denominations and applies no rate calculations.
 *
 * @deprecated Will be removed in a future version; prefer ILP Pay instead of Stream Sender functionality.
 */
@Deprecated
public class NoOpExchangeRateCalculator implements ExchangeRateCalculator {

  @Override
  public UnsignedLong calculateAmountToSend(UnsignedLong amountToSend,
    Denomination amountToSendDenomination,
    Denomination receiverDenomination) {
    return amountToSend;
  }

  @Override
  public UnsignedLong calculateMinAmountToAccept(
    final UnsignedLong sendAmount, final Denomination sendAmountDenomination
  ) {
    Objects.requireNonNull(sendAmount);
    Objects.requireNonNull(sendAmountDenomination);
    return UnsignedLong.ZERO;
  }
}
