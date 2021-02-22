package org.interledger.stream.calculators;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.stream.Denomination;
import org.interledger.stream.Denominations;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Unit tests for {@link ExchangeRateCalculator}.
 */
public class ExchangeRateCalculatorTest {

  @Test
  public void scaled() {
    final ExchangeRateCalculator calculator = new ExchangeRateCalculator() {
      @Override
      public UnsignedLong calculateAmountToSend(UnsignedLong amountToSend, Denomination amountToSendDenomination,
        Denomination receiverDenomination) throws NoExchangeRateException {
        return null;
      }

      @Override
      public UnsignedLong calculateMinAmountToAccept(UnsignedLong sendAmount, Denomination sendAmountDenomination)
        throws NoExchangeRateException {
        return null;
      }
    };
    assertThat(calculator.scaled(UnsignedLong.ONE, Denominations.USD_CENTS)).isEqualTo(BigDecimal.valueOf(.01));
  }

  @Test
  public void testCalculateMinAmountToAcceptDefault() {
    final AtomicBoolean called = new AtomicBoolean(false);
    final ExchangeRateCalculator calculator = new ExchangeRateCalculator() {
      @Override
      public UnsignedLong calculateAmountToSend(UnsignedLong amountToSend, Denomination amountToSendDenomination,
        Denomination receiverDenomination) throws NoExchangeRateException {
        return UnsignedLong.ZERO;
      }

      @Override
      public UnsignedLong calculateMinAmountToAccept(UnsignedLong sendAmount, Denomination sendAmountDenomination)
        throws NoExchangeRateException {
        called.set(true);
        return UnsignedLong.ZERO;
      }
    };
    calculator.calculateMinAmountToAccept(UnsignedLong.ZERO, Denominations.USD_CENTS);
    assertThat(called.get()).isTrue();
  }
}
