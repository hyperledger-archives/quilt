package org.interledger.stream.calculators;

import com.google.common.primitives.UnsignedLong;
import org.interledger.stream.Denomination;
import java.util.Optional;

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
