package org.interledger.stream.sender;

import com.google.common.primitives.UnsignedLong;
import java.util.Objects;
import org.interledger.stream.Denomination;
import org.interledger.stream.calculators.ExchangeRateCalculator;
import org.interledger.stream.calculators.NoExchangeRateException;

/**
 * An implementation of {@link ExchangeRateCalculator} that always assumes an exchange rate from sender:receiver
 * exchange rate of 2:1.
 */
public class HalfsiesExchangeRateCalculator implements ExchangeRateCalculator {

  @Override
  public UnsignedLong calculateAmountToSend(
    final UnsignedLong amountToSend,
    final Denomination amountToSendDenomination,
    final Denomination receiverDenomination
  ) throws NoExchangeRateException {
    Objects.requireNonNull(amountToSend);
    Objects.requireNonNull(amountToSendDenomination);
    Objects.requireNonNull(receiverDenomination);
    return amountToSend.times(UnsignedLong.valueOf(2));
  }

  @Override
  public UnsignedLong calculateMinAmountToAccept(
    final UnsignedLong sendAmount, final Denomination sendAmountDenomination
  ) {
    Objects.requireNonNull(sendAmount);
    Objects.requireNonNull(sendAmountDenomination);
    return sendAmount.dividedBy(UnsignedLong.valueOf(2));
  }
}