package org.interledger.stream.calculators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

import org.interledger.stream.Denomination;
import org.interledger.stream.Denominations;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Optional;

public class ExchangeRateCalculatorTest {

  @Test
  public void scaled() {
    final ExchangeRateCalculator calculator = new ExchangeRateCalculator() {
      @Override
      public UnsignedLong calculateAmountToSend(UnsignedLong amountToReceive, Denomination sendDenomination,
                                                Denomination receiveDenomination) throws NoExchangeRateException {
        return null;
      }

      @Override
      public UnsignedLong calculateMinAmountToAccept(UnsignedLong sendAmount,
                                                     Denomination sendDenomination,
                                                     Optional<Denomination> expectedReceivedDenomination)
          throws NoExchangeRateException {
        return null;
      }
    };
    assertThat(calculator.scaled(UnsignedLong.ONE, Denominations.US_CENTS)).isEqualTo(BigDecimal.valueOf(.01));
  }
}
