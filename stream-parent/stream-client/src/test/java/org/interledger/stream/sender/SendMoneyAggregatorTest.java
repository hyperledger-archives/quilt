package org.interledger.stream.sender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPacketType;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.SharedSecret;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.link.Link;
import org.interledger.stream.Denomination;
import org.interledger.stream.Denominations;
import org.interledger.stream.PaymentTracker;
import org.interledger.stream.PrepareAmounts;
import org.interledger.stream.SendMoneyRequest;
import org.interledger.stream.SenderAmountMode;
import org.interledger.stream.StreamConnection;
import org.interledger.stream.StreamConnectionClosedException;
import org.interledger.stream.StreamConnectionId;
import org.interledger.stream.StreamPacket;
import org.interledger.stream.calculators.NoOpExchangeRateCalculator;
import org.interledger.stream.crypto.StreamEncryptionService;
import org.interledger.stream.frames.StreamMoneyFrame;
import org.interledger.stream.sender.SimpleStreamSender.SendMoneyAggregator;

import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit tests for {@link SendMoneyAggregator}.
 */
public class SendMoneyAggregatorTest {

  // 5 seconds max per method tested
  @Rule
  public Timeout globalTimeout = Timeout.seconds(5);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private CodecContext streamCodecContextMock;
  @Mock
  private Link linkMock;
  @Mock
  private CongestionController congestionControllerMock;
  @Mock
  private StreamEncryptionService streamEncryptionServiceMock;
  @Mock
  private StreamConnection streamConnectionMock;

  private SharedSecret sharedSecret = SharedSecret.of(new byte[32]);
  private InterledgerAddress sourceAddress = InterledgerAddress.of("example.source");
  private InterledgerAddress destinationAddress = InterledgerAddress.of("example.destination");
  private UnsignedLong originalAmountToSend = UnsignedLong.valueOf(10L);

  private SendMoneyAggregator sendMoneyAggregator;
  private PaymentTracker paymentTracker;
  private PrepareAmounts defaultPrepareAmounts;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(congestionControllerMock.getMaxAmount()).thenReturn(UnsignedLong.ONE);
    when(streamEncryptionServiceMock.encrypt(any(), any())).thenReturn(new byte[32]);
    when(streamEncryptionServiceMock.decrypt(any(), any())).thenReturn(new byte[32]);
    when(linkMock.sendPacket(any())).thenReturn(mock(InterledgerRejectPacket.class));
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
    this.paymentTracker = new FixedSenderAmountPaymentTracker(originalAmountToSend, new NoOpExchangeRateCalculator());
    SendMoneyRequest request = SendMoneyRequest.builder()
        .sharedSecret(sharedSecret)
        .sourceAddress(sourceAddress)
        .senderAmountMode(SenderAmountMode.SENDER_AMOUNT)
        .destinationAddress(destinationAddress)
        .amount(originalAmountToSend)
        .timeout(Optional.of(Duration.ofSeconds(60)))
        .denomination(Denominations.XRP)
        .paymentTracker(paymentTracker)
        .build();
    this.sendMoneyAggregator = new SendMoneyAggregator(
        executor, streamConnectionMock, streamCodecContextMock, linkMock, congestionControllerMock,
        streamEncryptionServiceMock, request);

    defaultPrepareAmounts = PrepareAmounts.from(samplePreparePacket(), sampleStreamPacket());
  }

  @Test
  public void sendMoneyWhenTimedOut()
      throws ExecutionException, InterruptedException, StreamConnectionClosedException {

    setSoldierOnBooleans(false, false, false, true);
    when(streamConnectionMock.nextSequence()).thenReturn(UnsignedLong.ONE);

    sendMoneyAggregator.send().get();

    // Expect 1 Link call due to preflight check
    Mockito.verify(linkMock, times(1)).sendPacket(any());
  }

  @Test
  public void sendMoneyWhenMoreToSendButTimedOut()
      throws ExecutionException, InterruptedException, StreamConnectionClosedException {

    setSoldierOnBooleans(false, false, false, true);
    when(streamConnectionMock.nextSequence()).thenReturn(UnsignedLong.ONE);

    sendMoneyAggregator.send().get();

    // Expect 1 Link call due to preflight check
    Mockito.verify(linkMock, times(1)).sendPacket(any());
  }

  @Test
  public void sendMoneyWhenNoMoreToSend()
      throws ExecutionException, InterruptedException, StreamConnectionClosedException {

    setSoldierOnBooleans(false, false, false, false);
    when(streamConnectionMock.nextSequence()).thenReturn(UnsignedLong.ONE);

    sendMoneyAggregator.send().get();

    // Expect 1 Link call due to preflight check
    Mockito.verify(linkMock, times(1)).sendPacket(any());
  }

  @Test
  public void sendMoneyWhenConnectionIsClosed()
      throws ExecutionException, InterruptedException, StreamConnectionClosedException {

    setSoldierOnBooleans(false, true, true, false);
    when(streamConnectionMock.nextSequence())
        .thenReturn(StreamConnection.MAX_FRAMES_PER_CONNECTION.plus(UnsignedLong.ONE));

    sendMoneyAggregator.send().get();

    // Expect 1 Link call due to preflight check
    Mockito.verify(linkMock, times(1)).sendPacket(any());
  }

  @Test
  public void sendMoneyFailsPreflightSequenceIncrement()
      throws ExecutionException, InterruptedException, StreamConnectionClosedException {

    setSoldierOnBooleans(false, true, true, false);
    when(streamConnectionMock.nextSequence())
        .thenThrow(new StreamConnectionClosedException(StreamConnectionId.of("whoops")));

    sendMoneyAggregator.send().get();

    // Expect 0 link calls
    Mockito.verifyNoMoreInteractions(linkMock);
  }

  @Test
  public void sendMoneyWhenSequenceCannotIncrement()
      throws ExecutionException, InterruptedException, StreamConnectionClosedException {

    setSoldierOnBooleans(false, true, true, false);
    when(streamConnectionMock.nextSequence()).thenAnswer(new Answer<UnsignedLong>() {
      AtomicInteger counter = new AtomicInteger(0);

      @Override
      public UnsignedLong answer(InvocationOnMock invocationOnMock) throws Throwable {
        int i = counter.incrementAndGet();
        if (i > 1) {
          throw new StreamConnectionClosedException(StreamConnectionId.of("whoops"));
        }
        return UnsignedLong.ONE;
      }
    });

    sendMoneyAggregator.send().get();

    // Expect 1 Link call due to preflight check
    Mockito.verify(linkMock, times(1)).sendPacket(any());
  }

  @Test
  public void failureToSchedulePutsMoneyBack() {
    SendMoneyRequest request = SendMoneyRequest.builder()
        .sharedSecret(sharedSecret)
        .sourceAddress(sourceAddress)
        .amount(originalAmountToSend)
        .senderAmountMode(SenderAmountMode.SENDER_AMOUNT)
        .destinationAddress(destinationAddress)
        .timeout(Optional.of(Duration.ofSeconds(60)))
        .denomination(Denominations.XRP)
        .paymentTracker(new FixedSenderAmountPaymentTracker(originalAmountToSend, new NoOpExchangeRateCalculator()))
        .build();
    ExecutorService executor = mock(ExecutorService.class);
    this.sendMoneyAggregator = new SendMoneyAggregator(
        executor, streamConnectionMock, streamCodecContextMock, linkMock, congestionControllerMock,
        streamEncryptionServiceMock, request);

    when(executor.submit(any(Runnable.class))).thenThrow(new RejectedExecutionException());

    InterledgerPreparePacket prepare = samplePreparePacket();
    InterledgerRejectPacket expectedReject = InterledgerRejectPacket.builder()
        .code(InterledgerErrorCode.F00_BAD_REQUEST)
        .message(
            String.format("Unable to schedule sendMoney task. preparePacket=%s error=%s", prepare,
                "java.util.concurrent.RejectedExecutionException")
        )
        .build();

    expectedException.expect(RejectedExecutionException.class);
    sendMoneyAggregator.schedule(new AtomicBoolean(false), prepare, sampleStreamPacket(),
        PrepareAmounts.from(prepare, sampleStreamPacket()));
    verify(congestionControllerMock, times(1)).reject(UnsignedLong.ONE, expectedReject);
  }

  @Test
  public void preflightCheckFindsNoDenomination() throws Exception {
    when(streamConnectionMock.nextSequence()).thenReturn(UnsignedLong.ONE);
    when(linkMock.sendPacket(any())).thenReturn(sampleFulfillPacket());
    StreamPacket streamPacket = StreamPacket.builder().from(sampleStreamPacket())
        .addFrames(StreamMoneyFrame.builder()
            .shares(UnsignedLong.ONE)
            .streamId(UnsignedLong.ONE)
            .build())
        .build();
    when(streamCodecContextMock.read(any(), any())).thenReturn(streamPacket);

    Optional<Denomination> denomination = sendMoneyAggregator.preflightCheck();
    assertThat(denomination).isEmpty();
  }

  @Test
  public void preflightCheckRejects() throws Exception{
    when(streamConnectionMock.nextSequence()).thenReturn(UnsignedLong.ONE);
    when(linkMock.sendPacket(any())).thenReturn(sampleRejectPacket(InterledgerErrorCode.T00_INTERNAL_ERROR));
    StreamPacket streamPacket = StreamPacket.builder().from(sampleStreamPacket())
        .addFrames(StreamMoneyFrame.builder()
            .shares(UnsignedLong.ONE)
            .streamId(UnsignedLong.ONE)
            .build())
        .build();
    when(streamCodecContextMock.read(any(), any())).thenReturn(streamPacket);
    Optional<Denomination> denomination = sendMoneyAggregator.preflightCheck();
    assertThat(denomination).isEmpty();
  }

  @Test
  public void soldierOn() {
    // if money in flight, always soldier on
    // else
    //   you haven't reached max packets
    //   and you haven't delivered the full amount
    //   and you haven't timed out

    setSoldierOnBooleans(false, false, false, false);
    assertThat(sendMoneyAggregator.soldierOn(false)).isFalse();

    setSoldierOnBooleans(false, false, false, true);
    assertThat(sendMoneyAggregator.soldierOn(true)).isFalse();

    setSoldierOnBooleans(false, false, true, false);
    assertThat(sendMoneyAggregator.soldierOn(false)).isTrue();

    setSoldierOnBooleans(false, false, true, true);
    assertThat(sendMoneyAggregator.soldierOn(true)).isFalse();

    setSoldierOnBooleans(false, true, false, false);
    assertThat(sendMoneyAggregator.soldierOn(false)).isFalse();

    setSoldierOnBooleans(false, true, false, true);
    assertThat(sendMoneyAggregator.soldierOn(true)).isFalse();

    setSoldierOnBooleans(false, true, true, false);
    assertThat(sendMoneyAggregator.soldierOn(false)).isFalse();

    setSoldierOnBooleans(false, true, true, true);
    assertThat(sendMoneyAggregator.soldierOn(true)).isFalse();

    setSoldierOnBooleans(true, false, false, false);
    assertThat(sendMoneyAggregator.soldierOn(false)).isTrue();

    setSoldierOnBooleans(true, false, false, true);
    assertThat(sendMoneyAggregator.soldierOn(true)).isTrue();

    setSoldierOnBooleans(true, false, true, false);
    assertThat(sendMoneyAggregator.soldierOn(false)).isTrue();

    setSoldierOnBooleans(true, false, true, true);
    assertThat(sendMoneyAggregator.soldierOn(true)).isTrue();

    setSoldierOnBooleans(true, true, false, false);
    assertThat(sendMoneyAggregator.soldierOn(true)).isTrue();

    setSoldierOnBooleans(true, true, false, true);
    assertThat(sendMoneyAggregator.soldierOn(true)).isTrue();

    setSoldierOnBooleans(true, true, true, false);
    assertThat(sendMoneyAggregator.soldierOn(true)).isTrue();

    setSoldierOnBooleans(true, true, true, true);
    assertThat(sendMoneyAggregator.soldierOn(true)).isTrue();
  }

  @Test
  public void toEncrypted() throws Exception {
    doThrow(new IOException()).when(streamCodecContextMock).write(any(), any());
    expectedException.expect(StreamSenderException.class);
    StreamPacket packet = sampleStreamPacket();
    sendMoneyAggregator.toEncrypted(sharedSecret, packet);
  }

  @Test
  public void fromEncrypted() throws Exception {
    doThrow(new IOException()).when(streamCodecContextMock).read(any(), any());
    expectedException.expect(StreamSenderException.class);
    sendMoneyAggregator.fromEncrypted(sharedSecret, new byte[0]);
  }

  @Test
  public void handleRejectHatesNullPrepare() {
    expectedException.expect(NullPointerException.class);
    sendMoneyAggregator.handleReject(null, sampleStreamPacket(),
        sampleRejectPacket(InterledgerErrorCode.T00_INTERNAL_ERROR), defaultPrepareAmounts, new AtomicInteger(),
        congestionControllerMock);
  }

  @Test
  public void handleRejectHatesNullStreamPacket() {
    expectedException.expect(NullPointerException.class);
    sendMoneyAggregator.handleReject(samplePreparePacket(), null,
        sampleRejectPacket(InterledgerErrorCode.T00_INTERNAL_ERROR), defaultPrepareAmounts, new AtomicInteger(),
        congestionControllerMock);
  }

  @Test
  public void handleRejectHatesNullReject() {
    expectedException.expect(NullPointerException.class);
    sendMoneyAggregator.handleReject(null, sampleStreamPacket(), null, defaultPrepareAmounts,
        new AtomicInteger(), congestionControllerMock);
  }

  @Test
  public void handleRejectHatesNullNumReject() {
    expectedException.expect(NullPointerException.class);
    sendMoneyAggregator.handleReject(null, sampleStreamPacket(), sampleRejectPacket(InterledgerErrorCode.T00_INTERNAL_ERROR),
        null, null, congestionControllerMock);
  }

  @Test
  public void handleRejectHatesNullCongestionController() {
    expectedException.expect(NullPointerException.class);
    sendMoneyAggregator.handleReject(null, sampleStreamPacket(),
        sampleRejectPacket(InterledgerErrorCode.T00_INTERNAL_ERROR), defaultPrepareAmounts, new AtomicInteger(),
        null);
  }

  @Test
  public void handleRejectHatesNullPrepareAmountsr() {
    expectedException.expect(NullPointerException.class);
    sendMoneyAggregator.handleReject(null, sampleStreamPacket(),
        sampleRejectPacket(InterledgerErrorCode.T00_INTERNAL_ERROR), null, new AtomicInteger(),
        congestionControllerMock);
  }

  @Test
  public void handleReject() {
    UnsignedLong originalAmountToSend = paymentTracker.getOriginalAmountLeft();
    AtomicInteger numReject = new AtomicInteger(0);
    InterledgerPreparePacket prepare = samplePreparePacket();
    InterledgerRejectPacket reject = sampleRejectPacket(InterledgerErrorCode.T00_INTERNAL_ERROR);
    sendMoneyAggregator.handleReject(prepare, sampleStreamPacket(),
        reject, defaultPrepareAmounts, numReject, congestionControllerMock);
    assertThat(numReject.get()).isEqualTo(1);
    assertThat(paymentTracker.getOriginalAmountLeft()).isEqualTo(originalAmountToSend.plus(prepare.getAmount()));
    verify(congestionControllerMock, times(1)).reject(UnsignedLong.ONE, reject);
  }

  /**
   * Helper method to set the soldierOn mock values for clearer test coverage.
   */
  private void setSoldierOnBooleans(
      final boolean moneyInFlight, final boolean streamConnectionClosed, final boolean moreToSend,
      final boolean timeoutReached
  ) {
    when(congestionControllerMock.hasInFlight()).thenReturn(moneyInFlight);
    when(streamConnectionMock.isClosed()).thenReturn(streamConnectionClosed);
    FixedSenderAmountPaymentTracker fixedSender = (FixedSenderAmountPaymentTracker) this.paymentTracker;
    if (moreToSend) {
      fixedSender.setSentAmount(UnsignedLong.ZERO);
    } else {
      fixedSender.setSentAmount(UnsignedLong.valueOf(10L));
    }
  }

  private StreamPacket sampleStreamPacket() {
    return StreamPacket.builder()
        .prepareAmount(UnsignedLong.ZERO)
        .sequence(UnsignedLong.ZERO)
        .interledgerPacketType(InterledgerPacketType.PREPARE)
        .build();
  }

  private InterledgerPreparePacket samplePreparePacket() {
    return InterledgerPreparePacket.builder()
        .destination(destinationAddress)
        .amount(UnsignedLong.ONE)
        .expiresAt(Instant.now())
        .executionCondition(InterledgerCondition.of(new byte[32]))
        .build();
  }

  private InterledgerRejectPacket sampleRejectPacket(InterledgerErrorCode errorCode) {
    return InterledgerRejectPacket.builder()
        .code(errorCode)
        .message("too many cooks!")
        .triggeredBy(destinationAddress)
        .data(new byte[0])
        .build();
  }

  private InterledgerFulfillPacket sampleFulfillPacket() {
    return InterledgerFulfillPacket.builder()
        .data(new byte[0])
        .fulfillment(InterledgerFulfillment.of(new byte[32]))
        .build();
  }

}
