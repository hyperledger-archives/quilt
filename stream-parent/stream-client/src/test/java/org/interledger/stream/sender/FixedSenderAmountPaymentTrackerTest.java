package org.interledger.stream.sender;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.stream.Denominations;
import org.interledger.stream.PrepareAmounts;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

import java.util.Optional;

public class FixedSenderAmountPaymentTrackerTest {

  @Test
  public void checkAllInteractions() {
    FixedSenderAmountPaymentTracker tracker = new FixedSenderAmountPaymentTracker(UnsignedLong.valueOf(12l),
        new HalfsiesExchangeRateCalculator());

    assertThat(tracker.getAmountSent()).isEqualTo(UnsignedLong.ZERO);
    assertThat(tracker.getDeliveredAmount()).isEqualTo(UnsignedLong.ZERO);
    assertThat(tracker.getOriginalAmount()).isEqualTo(UnsignedLong.valueOf(12));
    assertThat(tracker.getOriginalAmountLeft()).isEqualTo(UnsignedLong.valueOf(12));
    assertThat(tracker.moreToSend()).isTrue();

    PrepareAmounts amounts = tracker.getSendPacketAmounts(UnsignedLong.ZERO, Denominations.XRP,
        Optional.of(Denominations.XRP));

    assertThat(amounts.getAmountToSend()).isEqualTo(UnsignedLong.ZERO);
    assertThat(amounts.getMinimumAmountToAccept()).isEqualTo(UnsignedLong.ZERO);

    amounts = tracker.getSendPacketAmounts(UnsignedLong.valueOf(6), Denominations.XRP,
        Optional.of(Denominations.XRP));

    assertThat(amounts.getAmountToSend()).isEqualTo(UnsignedLong.valueOf(6));
    assertThat(amounts.getMinimumAmountToAccept()).isEqualTo(UnsignedLong.valueOf(3));

    tracker.auth(amounts);
    assertThat(tracker.getAmountSent()).isEqualTo(UnsignedLong.ZERO);
    assertThat(tracker.getDeliveredAmount()).isEqualTo(UnsignedLong.ZERO);
    assertThat(tracker.getOriginalAmount()).isEqualTo(UnsignedLong.valueOf(12));
    assertThat(tracker.getOriginalAmountLeft()).isEqualTo(UnsignedLong.valueOf(6));
    assertThat(tracker.moreToSend()).isTrue();

    tracker.rollback(amounts, false);
    assertThat(tracker.getAmountSent()).isEqualTo(UnsignedLong.ZERO);
    assertThat(tracker.getDeliveredAmount()).isEqualTo(UnsignedLong.ZERO);
    assertThat(tracker.getOriginalAmount()).isEqualTo(UnsignedLong.valueOf(12));
    assertThat(tracker.getOriginalAmountLeft()).isEqualTo(UnsignedLong.valueOf(12));
    assertThat(tracker.moreToSend()).isTrue();

    tracker.auth(amounts);
    tracker.commit(amounts, UnsignedLong.valueOf(3));
    assertThat(tracker.getAmountSent()).isEqualTo(UnsignedLong.valueOf(6));
    assertThat(tracker.getDeliveredAmount()).isEqualTo(UnsignedLong.valueOf(3));
    assertThat(tracker.getOriginalAmount()).isEqualTo(UnsignedLong.valueOf(12));
    assertThat(tracker.getOriginalAmountLeft()).isEqualTo(UnsignedLong.valueOf(6));
    assertThat(tracker.moreToSend()).isTrue();

    tracker.auth(amounts);
    tracker.commit(amounts, UnsignedLong.valueOf(4));
    assertThat(tracker.getAmountSent()).isEqualTo(UnsignedLong.valueOf(12));
    assertThat(tracker.getDeliveredAmount()).isEqualTo(UnsignedLong.valueOf(7));
    assertThat(tracker.getOriginalAmount()).isEqualTo(UnsignedLong.valueOf(12));
    assertThat(tracker.getOriginalAmountLeft()).isEqualTo(UnsignedLong.ZERO);
    assertThat(tracker.moreToSend()).isFalse();

    assertThat(tracker.auth(amounts)).isFalse();
    assertThat(tracker.getAmountSent()).isEqualTo(UnsignedLong.valueOf(12));
    assertThat(tracker.getDeliveredAmount()).isEqualTo(UnsignedLong.valueOf(7));
    assertThat(tracker.getOriginalAmount()).isEqualTo(UnsignedLong.valueOf(12));
    assertThat(tracker.getOriginalAmountLeft()).isEqualTo(UnsignedLong.ZERO);
    assertThat(tracker.moreToSend()).isFalse();

    tracker.setSentAmount(UnsignedLong.valueOf(20));
    assertThat(tracker.getAmountSent()).isEqualTo(UnsignedLong.valueOf(20));

  }
}
