package org.interledger.stream.sender;

import org.interledger.stream.Denomination;
import org.interledger.stream.calculators.ExchangeRateCalculator;
import org.interledger.stream.calculators.NoExchangeRateException;

import com.google.common.primitives.UnsignedLong;

import java.util.Optional;

/**
 * An implementation of {@link ExchangeRateCalculator} that always assumes an exchange rate from sender:receiver
 * exchange rate of 2:1.
 */
public class HalfsiesExchangeRateCalculator implements ExchangeRateCalculator {

  @Override
  public UnsignedLong calculateAmountToSend(UnsignedLong amountToReceive, Denomination sendDenomination,
      Denomination receiveDenomination) throws NoExchangeRateException {
    return amountToReceive.times(UnsignedLong.valueOf(2));
  }

  @Override
  public UnsignedLong calculateMinAmountToAccept(UnsignedLong sendAmount, Denomination sendDenomination,
      Optional<Denomination> expectedReceivedDenomination)
      throws NoExchangeRateException {
    return sendAmount.dividedBy(UnsignedLong.valueOf(2));
  }
}
