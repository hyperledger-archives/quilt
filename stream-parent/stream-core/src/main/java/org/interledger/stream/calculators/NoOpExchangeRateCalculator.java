package org.interledger.stream.calculators;

import com.google.common.primitives.UnsignedLong;
import org.interledger.stream.Denomination;
import java.util.Optional;

public class NoOpExchangeRateCalculator implements ExchangeRateCalculator {

  @Override
  public UnsignedLong calculate(UnsignedLong sendAmount, Denomination sendDenomination,
                                Optional<Denomination> expectedReceivedDenomination) {
    return UnsignedLong.ZERO;
  }

}
