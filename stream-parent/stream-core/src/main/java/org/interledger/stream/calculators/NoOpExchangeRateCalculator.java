package org.interledger.stream.calculators;

import org.interledger.stream.Denomination;

import com.google.common.primitives.UnsignedLong;

import java.util.Optional;

/**
 * Exchange rate calculator that ignores denominations and applies no rate calculations.
 */
public class NoOpExchangeRateCalculator implements ExchangeRateCalculator {


  @Override
  public UnsignedLong calculateAmountToSend(UnsignedLong amountToReceive,
                                            Denomination sendDenomination,
                                            Denomination receiveDenomination) {
    return amountToReceive;
  }

  @Override
  public UnsignedLong calculateMinAmountToAccept(UnsignedLong sendAmount, Denomination sendDenomination,
                                                 Optional<Denomination> expectedReceivedDenomination) {
    return UnsignedLong.ZERO;
  }

}
