package org.interledger.stream.sender;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.stream.Denominations;
import org.interledger.stream.PrepareAmounts;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

import java.util.Optional;

public class FixedReceiverAmountPaymentTrackerTest {

  @Test
  public void checkAllInteractions() {
    FixedReceiverAmountPaymentTracker tracker = new FixedReceiverAmountPaymentTracker(UnsignedLong.valueOf(10L),
        new HalfsiesExchangeRateCalculator());

    assertThat(tracker.getDeliveredAmountInSenderUnits()).isEqualTo(UnsignedLong.ZERO);
    assertThat(tracker.getDeliveredAmountInReceiverUnits()).isEqualTo(UnsignedLong.ZERO);
    assertThat(tracker.getOriginalAmount()).isEqualTo(UnsignedLong.valueOf(10));
    assertThat(tracker.getOriginalAmountLeft()).isEqualTo(UnsignedLong.valueOf(10));
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
    assertThat(tracker.getDeliveredAmountInSenderUnits()).isEqualTo(UnsignedLong.ZERO);
    assertThat(tracker.getDeliveredAmountInReceiverUnits()).isEqualTo(UnsignedLong.ZERO);
    assertThat(tracker.getOriginalAmount()).isEqualTo(UnsignedLong.valueOf(10));
    assertThat(tracker.getOriginalAmountLeft()).isEqualTo(UnsignedLong.valueOf(7));
    assertThat(tracker.moreToSend()).isTrue();

    tracker.rollback(amounts, false);
    assertThat(tracker.getDeliveredAmountInSenderUnits()).isEqualTo(UnsignedLong.ZERO);
    assertThat(tracker.getDeliveredAmountInReceiverUnits()).isEqualTo(UnsignedLong.ZERO);
    assertThat(tracker.getOriginalAmount()).isEqualTo(UnsignedLong.valueOf(10));
    assertThat(tracker.getOriginalAmountLeft()).isEqualTo(UnsignedLong.valueOf(10));
    assertThat(tracker.moreToSend()).isTrue();

    tracker.auth(amounts);
    tracker.commit(amounts, UnsignedLong.valueOf(3));
    assertThat(tracker.getDeliveredAmountInSenderUnits()).isEqualTo(UnsignedLong.valueOf(6));
    assertThat(tracker.getDeliveredAmountInReceiverUnits()).isEqualTo(UnsignedLong.valueOf(3));
    assertThat(tracker.getOriginalAmount()).isEqualTo(UnsignedLong.valueOf(10));
    assertThat(tracker.getOriginalAmountLeft()).isEqualTo(UnsignedLong.valueOf(7));
    assertThat(tracker.moreToSend()).isTrue();

    tracker.auth(amounts);
    tracker.commit(amounts, UnsignedLong.valueOf(4));
    assertThat(tracker.getDeliveredAmountInSenderUnits()).isEqualTo(UnsignedLong.valueOf(12));
    assertThat(tracker.getDeliveredAmountInReceiverUnits()).isEqualTo(UnsignedLong.valueOf(7));
    assertThat(tracker.getOriginalAmount()).isEqualTo(UnsignedLong.valueOf(10));
    assertThat(tracker.getOriginalAmountLeft()).isEqualTo(UnsignedLong.valueOf(3));
    assertThat(tracker.moreToSend()).isTrue();

    tracker.auth(amounts);
    tracker.commit(amounts, UnsignedLong.valueOf(3));
    assertThat(tracker.getDeliveredAmountInSenderUnits()).isEqualTo(UnsignedLong.valueOf(18));
    assertThat(tracker.getDeliveredAmountInReceiverUnits()).isEqualTo(UnsignedLong.valueOf(10));
    assertThat(tracker.getOriginalAmount()).isEqualTo(UnsignedLong.valueOf(10));
    assertThat(tracker.getOriginalAmountLeft()).isEqualTo(UnsignedLong.ZERO);
    assertThat(tracker.moreToSend()).isFalse();

    tracker.auth(amounts);
    assertThat(tracker.getDeliveredAmountInSenderUnits()).isEqualTo(UnsignedLong.valueOf(18));
    assertThat(tracker.getDeliveredAmountInReceiverUnits()).isEqualTo(UnsignedLong.valueOf(10));
    assertThat(tracker.getOriginalAmount()).isEqualTo(UnsignedLong.valueOf(10));
    assertThat(tracker.getOriginalAmountLeft()).isEqualTo(UnsignedLong.ZERO);
    assertThat(tracker.moreToSend()).isFalse();

    amounts = tracker.getSendPacketAmounts(UnsignedLong.valueOf(6), Denominations.XRP,
        Optional.of(Denominations.XRP));

    assertThat(amounts.getAmountToSend()).isEqualTo(UnsignedLong.ZERO);
    assertThat(amounts.getMinimumAmountToAccept()).isEqualTo(UnsignedLong.ZERO);
  }

}
