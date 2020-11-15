package org.interledger.stream.sender.good;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.link.Link;
import org.interledger.stream.good.SendMoneyRequest;

import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class DefaultRunLoopTest {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Mock
  private Link linkMock;

  @Mock
  private PaymentTracker paymentTrackerMock;

  private RunLoopStateMachine runLoopStateMachine;

  private SendMoneyRequest sendMoneyRequest;
  private Supplier<InterledgerPreparePacket> preparePacketSupplier;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(linkMock.sendPacket(any())).thenReturn(
        InterledgerFulfillPacket.builder().fulfillment(InterledgerFulfillment.of(new byte[32])).build()
    );

// TODO: FIXME
//    this.sendMoneyRequest = SendMoneyRequest.builder()
//        .senderAddress(InterledgerAddress.of("private.sender"))
//        .senderDenomination(Denomination.builder().assetCode("USD").assetScale((short) 2).build())
//        .senderAmount(UnsignedLong.ONE)
//        .destinationAddress(InterledgerAddress.of("private.destination"))
//        .receiverDenomination(Denomination.builder().assetCode("USD").assetScale((short) 2).build())
//        .paymentTimeout(Duration.of(30, ChronoUnit.SECONDS))
//        .sharedSecret(SharedSecret.of(new byte[32]))
//        .build();

    this.preparePacketSupplier = () -> InterledgerPreparePacket.builder()
        .amount(UnsignedLong.ONE)
        .destination(InterledgerAddress.of("private.destination"))
        .executionCondition(InterledgerCondition.of(new byte[32]))
        .expiresAt(Instant.now().plus(30, ChronoUnit.SECONDS))
        .build();

    // TODO: FIXME
//    this.runLoopStateMachine = new RunLoopStateMachine(
//        paymentTrackerMock,
//        sendMoneyRequest,
//        linkMock,
//        preparePacketSupplier
//      //  runLoopPackets, fulfillPacketHandler, rejectPacketHandler
//    );
  }

  @Test
  public void testPaymentWithNPackets() throws InterruptedException {
    for (int i = 1; i < 100; i++) {
      testPaymentWithNPacketsHelper(i);
    }
  }

  /**
   * Sends two packets that take 10 seconds to complete. However, the time-since-last-fulfill is marked as timed-out
   * after 500ms, so we expect all threads to be cancelled.
   */
  @Test
  public void testPaymentThatTimesOutDueToLastFulfill() throws InterruptedException {
    final AtomicInteger amountInFlight = new AtomicInteger(1);

    when(paymentTrackerMock.moreToSend()).thenReturn(true);
//    when(paymentTrackerMock.isMaxInFlight()).thenAnswer((Answer<Boolean>) invocation -> {
//      if (amountInFlight.get() > 0) {
//        return true;
//      } else {
//        return false;
//      }
//    });

    // Use the latch to make sure the test waits for anything in the completeable future.
    when(linkMock.sendPacket(any())).thenAnswer((Answer<InterledgerResponsePacket>) invocation -> {
      amountInFlight.getAndIncrement();
      logger.info("SEND_PACKET {} (should take 10 seconds unless max_time_since_last_fulfill is hit)");
      Thread.sleep(10000);
      return InterledgerFulfillPacket.builder().fulfillment(InterledgerFulfillment.of(new byte[32])).build();
    });

    Instant timeout = Instant.now().minus(29500, ChronoUnit.MILLIS);
    when(paymentTrackerMock.getLastFulfillTime()).thenReturn(new AtomicReference<>(timeout));

    this.runLoopStateMachine.start();

    // TODO: Assert the PaymentState fater the loop

    // We don't verify the link because depending on the speed of the system running this test, the send operation may
    // or may not succeed.
    //verify(paymentTrackerMock, times(2)).isMaxInFlight();
    verify(paymentTrackerMock, times(2)).moreToSend();
    verify(paymentTrackerMock, times(3)).getLastFulfillTime();
  }

  /**
   * Sends two packets that take 10 seconds to complete. However, the payment is marked as having an overall timeout of
   * 500 ms, so we expect all threads to be cancelled.
   */
  @Test
  public void testPaymentThatTimesOutDueToOverallTimeout() throws InterruptedException {
    final AtomicInteger amountInFlight = new AtomicInteger(1);

    when(paymentTrackerMock.moreToSend()).thenAnswer((Answer<Boolean>) invocation -> {
      if (amountInFlight.getAndDecrement() > 0) {
        return true;
      } else {
        return false;
      }
    });
   // when(paymentTrackerMock.isMaxInFlight()).thenReturn(false);

    // Use the latch to make sure the test waits for anything in the completeable future.
    when(linkMock.sendPacket(any())).thenAnswer((Answer<InterledgerResponsePacket>) invocation -> {
      amountInFlight.getAndIncrement();
      Thread.sleep(10000);
      logger.info("SEND_PACKET {} (SHOULD NOT GET CALLED because the thread should be cancelled first.)");
      return InterledgerFulfillPacket.builder().fulfillment(InterledgerFulfillment.of(new byte[32])).build();
    });

    when(paymentTrackerMock.getLastFulfillTime()).thenReturn(new AtomicReference<>(Instant.now()));

    // TODO: FIXME
//    this.runLoopStateMachine = new RunLoopStateMachine(
//        paymentTrackerMock,
//        SendMoneyRequest.builder().from(this.sendMoneyRequest)
//            // Only allow 500ms to send the payment before it times out.
//            .paymentTimeout(Duration.of(500, ChronoUnit.MILLIS))
//            .build(),
//        linkMock,
//        preparePacketSupplier,
//        //runLoopPackets, fulfillPacketHandler, rejectPacketHandler
//    );

    this.runLoopStateMachine.start();

    // TODO: Assert the PaymentState fater the loop

    // We don't verify the link because depending on the speed of the system running this test, the send operation may
    // or may not succeed.
    //verify(paymentTrackerMock, times(1)).isMaxInFlight();
    verify(paymentTrackerMock, times(2)).moreToSend();
    verify(paymentTrackerMock, times(2)).getLastFulfillTime();
  }

  /**
   * Sends two packets that take 10 seconds to complete. However, the payment is marked as having an overall timeout of
   * 500 ms, so we expect all threads to be cancelled.
   */
  @Test
  public void testPaymentIsFailing() throws InterruptedException {
    final AtomicInteger numPacketsSent = new AtomicInteger();

    when(paymentTrackerMock.getLastFulfillTime()).thenReturn(new AtomicReference<>(Instant.now()));
    when(paymentTrackerMock.moreToSend()).thenAnswer((Answer<Boolean>) invocation -> {
      if (numPacketsSent.get() < 1) {
        return true;
      } else {
        return false;
      }
    });
    //when(paymentTrackerMock.isMaxInFlight()).thenReturn(false);
    when(paymentTrackerMock.isPaymentFailing()).thenAnswer((Answer<Boolean>) invocation -> {
      if (numPacketsSent.get() > 1) {
        return false;
      } else {
        return true;
      }
    });
    when(linkMock.sendPacket(any())).thenAnswer((Answer<InterledgerResponsePacket>) invocation -> {
      //amountInFlight.getAndDecrement();
      Thread.sleep(500);
      logger.info("SEND_PACKET {} (SHOULD NOT GET CALLED because the thread should be cancelled first.)");
      numPacketsSent.incrementAndGet();
      return InterledgerFulfillPacket.builder().fulfillment(InterledgerFulfillment.of(new byte[32])).build();
    });

    this.runLoopStateMachine.start();

    verify(paymentTrackerMock, times(2)).getLastFulfillTime();
    verify(paymentTrackerMock, times(2)).isPaymentFailing();
    verifyNoMoreInteractions(paymentTrackerMock);
  }

  /////////////////
  // Helper methods
  /////////////////

  private void testPaymentWithNPacketsHelper(final int numPackets) throws InterruptedException {
    CountDownLatch latch = this.configureForNPackets(numPackets);

    this.runLoopStateMachine.start();

    latch.await();
    verify(paymentTrackerMock, times(numPackets + 1)).moreToSend();
    verify(linkMock, times(numPackets)).sendPacket(any());
  }

  /**
   * Mocks the PaymentTracker so that the Loop machine sends the proper number of packets for the simulation.
   */
  private CountDownLatch configureForNPackets(int numPackets) {
    Mockito.reset(paymentTrackerMock, linkMock);
// TODO: FIXME
    //    this.runLoopStateMachine = new RunLoopStateMachine(
//        paymentTrackerMock,
//        sendMoneyRequest,
//        linkMock,
//        preparePacketSupplier,
//        runLoopPackets, fulfillPacketHandler, rejectPacketHandler
//    );
    when(paymentTrackerMock.getLastFulfillTime()).thenReturn(new AtomicReference<>(Instant.now()));
    final AtomicInteger numLeftToSend = new AtomicInteger(numPackets);

    final CountDownLatch latch = new CountDownLatch(numPackets);
    when(paymentTrackerMock.moreToSend()).thenAnswer((Answer<Boolean>) invocation -> {
      if (numLeftToSend.getAndDecrement() <= 0) {
        return false; // No more to send
      } else {
        return true; // More to send
      }
    });

    // Use the latch to make sure the test waits for anything in the completeable future.
    when(linkMock.sendPacket(any())).thenAnswer((Answer<InterledgerResponsePacket>) invocation -> {
      logger.info("SEND_PACKET {}", numPackets);
      latch.countDown();
      return InterledgerFulfillPacket.builder().fulfillment(InterledgerFulfillment.of(new byte[32])).build();
    });
    return latch;
  }
}
