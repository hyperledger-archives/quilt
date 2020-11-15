package org.interledger.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.stream.SenderAmountMode.RECEIVER_AMOUNT;
import static org.interledger.stream.SenderAmountMode.SENDER_AMOUNT;

import com.google.common.primitives.UnsignedLong;
import org.interledger.fx.Denomination;
import org.junit.Test;

/**
 * Unit tests for default methods in {@link PaymentTracker}.
 */
public class PaymentTrackerTest {

  @Test
  public void testGetOriginalAmountMode() {
    PaymentTracker<?> paymentTracker = constructPaymentTracker(SENDER_AMOUNT);
    assertThat(paymentTracker.getOriginalAmountMode()).isEqualTo(SENDER_AMOUNT);
    assertThat(paymentTracker.requiresReceiverDenomination()).isFalse();

    paymentTracker = constructPaymentTracker(RECEIVER_AMOUNT);
    assertThat(paymentTracker.getOriginalAmountMode()).isEqualTo(RECEIVER_AMOUNT);
    assertThat(paymentTracker.requiresReceiverDenomination()).isTrue();
  }

  @Test
  public void testIsSuccessful() {
    PaymentTracker<?> paymentTracker = constructPaymentTracker(SENDER_AMOUNT, false);
    assertThat(paymentTracker.successful()).isTrue();

    paymentTracker = constructPaymentTracker(SENDER_AMOUNT, true);
    assertThat(paymentTracker.successful()).isFalse();
  }

  //////////////////
  // Private Helpers
  //////////////////

  private PaymentTracker<?> constructPaymentTracker(final SenderAmountMode originalAmountMode) {
    return constructPaymentTracker(originalAmountMode, false);
  }

  private PaymentTracker<?> constructPaymentTracker(final SenderAmountMode originalAmountMode,
    final boolean moreToSend) {
    return new PaymentTracker() {
      @Override
      public UnsignedLong getOriginalAmount() {
        return UnsignedLong.ZERO;
      }

      @Override
      public SenderAmountMode getOriginalAmountMode() {
        return originalAmountMode;
      }

      @Override
      public UnsignedLong getOriginalAmountLeft() {
        return UnsignedLong.ZERO;
      }

      @Override
      public UnsignedLong getDeliveredAmountInSenderUnits() {
        return UnsignedLong.ZERO;
      }

      @Override
      public UnsignedLong getDeliveredAmountInReceiverUnits() {
        return UnsignedLong.ZERO;
      }

      @Override
      public PrepareAmounts getSendPacketAmounts(UnsignedLong congestionLimit, Denomination senderDenomination) {
        return null;
      }

      @Override
      public PrepareAmounts getSendPacketAmounts(
        UnsignedLong congestionLimit, Denomination senderDenomination, Denomination receiverDenomination
      ) {
        return null;
      }

      @Override
      public boolean auth(PrepareAmounts prepareAmounts) {
        return false;
      }

      @Override
      public void rollback(PrepareAmounts prepareAmounts, boolean packetRejected) {

      }

      @Override
      public void commit(PrepareAmounts prepareAmounts, UnsignedLong deliveredAmount) {

      }

      @Override
      public boolean moreToSend() {
        return moreToSend;
      }
    };
  }
}
