package org.interledger.stream.sender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.core.InterledgerErrorCode.F00_BAD_REQUEST;
import static org.interledger.core.InterledgerErrorCode.F01_INVALID_PACKET;
import static org.interledger.core.InterledgerErrorCode.F02_UNREACHABLE;
import static org.interledger.core.InterledgerErrorCode.F03_INVALID_AMOUNT;
import static org.interledger.core.InterledgerErrorCode.F04_INSUFFICIENT_DST_AMOUNT;
import static org.interledger.core.InterledgerErrorCode.F05_WRONG_CONDITION;
import static org.interledger.core.InterledgerErrorCode.F06_UNEXPECTED_PAYMENT;
import static org.interledger.core.InterledgerErrorCode.F07_CANNOT_RECEIVE;
import static org.interledger.core.InterledgerErrorCode.F08_AMOUNT_TOO_LARGE;
import static org.interledger.core.InterledgerErrorCode.F99_APPLICATION_ERROR;
import static org.interledger.core.InterledgerErrorCode.R00_TRANSFER_TIMED_OUT;
import static org.interledger.core.InterledgerErrorCode.R01_INSUFFICIENT_SOURCE_AMOUNT;
import static org.interledger.core.InterledgerErrorCode.R02_INSUFFICIENT_TIMEOUT;
import static org.interledger.core.InterledgerErrorCode.R99_APPLICATION_ERROR;
import static org.interledger.core.InterledgerErrorCode.T00_INTERNAL_ERROR;
import static org.interledger.core.InterledgerErrorCode.T01_PEER_UNREACHABLE;
import static org.interledger.core.InterledgerErrorCode.T02_PEER_BUSY;
import static org.interledger.core.InterledgerErrorCode.T03_CONNECTOR_BUSY;
import static org.interledger.core.InterledgerErrorCode.T04_INSUFFICIENT_LIQUIDITY;
import static org.interledger.core.InterledgerErrorCode.T05_RATE_LIMITED;
import static org.interledger.core.InterledgerErrorCode.T99_APPLICATION_ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.interledger.core.DateUtils;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPacketType;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.SharedSecret;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.link.Link;
import org.interledger.stream.Denomination;
import org.interledger.stream.Denominations;
import org.interledger.stream.PaymentTracker;
import org.interledger.stream.PrepareAmounts;
import org.interledger.stream.SendMoneyRequest;
import org.interledger.stream.SendMoneyResult;
import org.interledger.stream.SenderAmountMode;
import org.interledger.stream.StreamConnection;
import org.interledger.stream.StreamConnectionId;
import org.interledger.stream.StreamPacket;
import org.interledger.stream.calculators.NoOpExchangeRateCalculator;
import org.interledger.stream.crypto.StreamEncryptionService;
import org.interledger.stream.StreamConnectionClosedException;
import org.interledger.stream.frames.ErrorCodes;
import org.interledger.stream.frames.StreamCloseFrame;
import org.interledger.stream.frames.StreamFrame;
import org.interledger.stream.frames.StreamMoneyFrame;
import org.interledger.stream.sender.SimpleStreamSender.SendMoneyAggregator;

import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Unit tests for {@link SendMoneyAggregator}.
 */
@SuppressWarnings("deprecation")
public class SendMoneyAggregatorTest {

  // 5 seconds max per method tested
  @Rule
  public Timeout globalTimeout = Timeout.seconds(600);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private CodecContext streamCodecContextMock;
  @Mock
  private Link<?> linkMock;
  @Mock
  private CongestionController congestionControllerMock;
  @Mock
  private StreamEncryptionService streamEncryptionServiceMock;
  @Mock
  private StreamConnection streamConnectionMock;

  private final SharedSecret sharedSecret = SharedSecret.of(new byte[32]);
  private final InterledgerAddress sourceAddress = InterledgerAddress.of("example.source");
  private final InterledgerAddress destinationAddress = InterledgerAddress.of("example.destination");
  private final UnsignedLong originalAmountToSend = UnsignedLong.valueOf(10L);

  private SendMoneyAggregator sendMoneyAggregator;
  private PaymentTracker paymentTracker;
  private PrepareAmounts defaultPrepareAmounts;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(congestionControllerMock.getMaxAmount()).thenReturn(UnsignedLong.ONE);
    when(streamEncryptionServiceMock.encrypt(any(), any())).thenReturn(new byte[32]);
    when(streamEncryptionServiceMock.decrypt(any(), any())).thenReturn(new byte[32]);
    when(linkMock.sendPacket(any())).thenReturn(sampleRejectPacket(F99_APPLICATION_ERROR));
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
    this.paymentTracker = new FixedSenderAmountPaymentTracker(originalAmountToSend, new NoOpExchangeRateCalculator());
    @SuppressWarnings("unchecked") SendMoneyRequest request = SendMoneyRequest.builder()
      .sharedSecret(sharedSecret)
      .sourceAddress(sourceAddress)
      .destinationAddress(destinationAddress)
      .amount(originalAmountToSend)
      .timeout(Optional.of(Duration.ofSeconds(60)))
      .denomination(Denominations.XRP_DROPS)
      .paymentTracker(paymentTracker)
      .build();
    this.sendMoneyAggregator = new SendMoneyAggregator(
      executor, streamConnectionMock, streamCodecContextMock, linkMock, congestionControllerMock,
      streamEncryptionServiceMock, Duration.ofMillis(10L), request
    );

    defaultPrepareAmounts = PrepareAmounts.from(samplePreparePacket(), sampleStreamPacket());
  }

  @Test
  public void sendMoneyWhenTimedOut()
    throws ExecutionException, InterruptedException, StreamConnectionClosedException {

    setSoldierOnBooleans(false, false, false);
    when(streamConnectionMock.nextSequence()).thenReturn(UnsignedLong.ONE);

    sendMoneyAggregator.send().get();

    assertOnlyPreflightAndClose();
  }

  @Test
  public void sendMoneyWhenMoreToSendButTimedOut()
    throws ExecutionException, InterruptedException, StreamConnectionClosedException {

    setSoldierOnBooleans(false, false, false);
    when(streamConnectionMock.nextSequence()).thenReturn(UnsignedLong.ONE);

    sendMoneyAggregator.send().get();

    assertOnlyPreflightAndClose();
  }

  @Test
  public void sendMoneyWhenNoMoreToSend()
    throws ExecutionException, InterruptedException, StreamConnectionClosedException {

    setSoldierOnBooleans(false, false, false);
    when(streamConnectionMock.nextSequence()).thenReturn(UnsignedLong.ONE);

    sendMoneyAggregator.send().get();
    assertOnlyPreflightAndClose();
  }

  @Test
  public void sendMoneyWhenConnectionIsClosed()
    throws ExecutionException, InterruptedException, StreamConnectionClosedException {

    setSoldierOnBooleans(false, true, true);
    when(streamConnectionMock.nextSequence())
      .thenReturn(StreamConnection.MAX_FRAMES_PER_CONNECTION.plus(UnsignedLong.ONE));

    sendMoneyAggregator.send().get();

    assertOnlyPreflightAndClose();
  }

  @Test
  public void sendMoneyFailsPreflightSequenceIncrement()
    throws ExecutionException, InterruptedException, StreamConnectionClosedException {

    setSoldierOnBooleans(false, true, true);
    when(streamConnectionMock.nextSequence())
      .thenThrow(new StreamConnectionClosedException(StreamConnectionId.of("whoops")));

    sendMoneyAggregator.send().get();

    // Expect 0 link calls
    Mockito.verify(linkMock).getOperatorAddressSupplier();
    Mockito.verifyNoMoreInteractions(linkMock);
  }

  @Test
  public void sendMoneyWhenSequenceCannotIncrement()
    throws ExecutionException, InterruptedException, StreamConnectionClosedException {

    setSoldierOnBooleans(false, true, true);
    when(streamConnectionMock.nextSequence()).thenAnswer(new Answer<UnsignedLong>() {
      final AtomicInteger counter = new AtomicInteger(0);

      @Override
      public UnsignedLong answer(InvocationOnMock invocationOnMock) {
        int incrementedCounter = counter.incrementAndGet();
        if (incrementedCounter > 1) {
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
      .destinationAddress(destinationAddress)
      .timeout(Optional.of(Duration.ofSeconds(60)))
      .denomination(Denominations.XRP_DROPS)
      .paymentTracker(new FixedSenderAmountPaymentTracker(originalAmountToSend, new NoOpExchangeRateCalculator()))
      .build();
    ExecutorService executor = mock(ExecutorService.class);
    this.sendMoneyAggregator = new SendMoneyAggregator(
      executor, streamConnectionMock, streamCodecContextMock, linkMock, congestionControllerMock,
      streamEncryptionServiceMock, Duration.ofSeconds(10L), request
    );

    when(executor.submit(any(Runnable.class))).thenThrow(new RejectedExecutionException());

    InterledgerPreparePacket prepare = samplePreparePacket();
    InterledgerRejectPacket expectedReject = InterledgerRejectPacket.builder()
      .code(F00_BAD_REQUEST)
      .message(
        String.format("Unable to schedule sendMoney task. preparePacket=%s error=%s", prepare,
          "java.util.concurrent.RejectedExecutionException")
      )
      .build();

    expectedException.expect(RejectedExecutionException.class);
    sendMoneyAggregator.schedule(new AtomicBoolean(false), () -> prepare, sampleStreamPacket(),
      PrepareAmounts.from(prepare, sampleStreamPacket()));
    verify(congestionControllerMock, times(1)).onReject(UnsignedLong.ONE, expectedReject);
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
  public void preflightCheckRejects() throws Exception {
    when(streamConnectionMock.nextSequence()).thenReturn(UnsignedLong.ONE);
    when(linkMock.sendPacket(any())).thenReturn(sampleRejectPacket(T00_INTERNAL_ERROR));
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
    //   and you haven't tried to send too much
    //   and we haven't hit an unrecoverable error

    setSoldierOnBooleans(false, false, false);
    allSoldierOnsFalse();

    setSoldierOnBooleans(false, false, true);
    assertThat(sendMoneyAggregator.soldierOn(false, false)).isTrue();
    assertThat(sendMoneyAggregator.soldierOn(true, false)).isFalse();
    assertThat(sendMoneyAggregator.soldierOn(false, true)).isFalse();
    assertThat(sendMoneyAggregator.soldierOn(true, true)).isFalse();

    setSoldierOnBooleans(false, true, false);
    allSoldierOnsFalse();

    setSoldierOnBooleans(false, true, true);
    allSoldierOnsFalse();

    setSoldierOnBooleans(true, false, false);
    allSoldierOnsTrue();

    setSoldierOnBooleans(true, false, true);
    allSoldierOnsTrue();

    setSoldierOnBooleans(true, true, false);
    allSoldierOnsTrue();

    setSoldierOnBooleans(true, true, true);
    allSoldierOnsTrue();

    // flip flag on unrecoverable error
    sendMoneyAggregator.setUnrecoverableErrorEncountered(true);

    setSoldierOnBooleans(false, false, false);
    allSoldierOnsFalse();
    setSoldierOnBooleans(false, false, true);
    allSoldierOnsFalse();
  }

  private void allSoldierOnsTrue() {
    assertThat(sendMoneyAggregator.soldierOn(false, false)).isTrue();
    assertThat(sendMoneyAggregator.soldierOn(true, false)).isTrue();
    assertThat(sendMoneyAggregator.soldierOn(false, true)).isTrue();
    assertThat(sendMoneyAggregator.soldierOn(true, true)).isTrue();
  }

  private void allSoldierOnsFalse() {
    assertThat(sendMoneyAggregator.soldierOn(false, false)).isFalse();
    assertThat(sendMoneyAggregator.soldierOn(true, false)).isFalse();
    assertThat(sendMoneyAggregator.soldierOn(false, true)).isFalse();
    assertThat(sendMoneyAggregator.soldierOn(true, true)).isFalse();
  }

  @Test
  public void breakLoopToPreventOversend() throws Exception {
    PaymentTracker tracker = mock(PaymentTracker.class);
    PrepareAmounts prepare = PrepareAmounts.builder()
      .amountToSend(UnsignedLong.valueOf(10L))
      .minimumAmountToAccept(UnsignedLong.ZERO)
      .build();
    when(tracker.getSendPacketAmounts(any(), any())).thenReturn(prepare);
    when(tracker.auth(any())).thenReturn(false);
    when(tracker.getOriginalAmountMode()).thenReturn(SenderAmountMode.SENDER_AMOUNT);
    when(tracker.moreToSend()).thenReturn(true);
    when(tracker.getDeliveredAmountInReceiverUnits()).thenReturn(UnsignedLong.ZERO);
    when(tracker.getDeliveredAmountInSenderUnits()).thenReturn(UnsignedLong.ZERO);
    when(tracker.getOriginalAmount()).thenReturn(UnsignedLong.valueOf(10));
    when(tracker.getOriginalAmountLeft()).thenReturn(UnsignedLong.valueOf(10));
    when(tracker.successful()).thenReturn(false);

    @SuppressWarnings("unchecked") SendMoneyRequest request = SendMoneyRequest.builder()
      .sharedSecret(sharedSecret)
      .sourceAddress(sourceAddress)
      .destinationAddress(destinationAddress)
      .amount(originalAmountToSend)
      .timeout(Optional.of(Duration.ofSeconds(60)))
      .denomination(Denominations.XRP_DROPS)
      .paymentTracker(tracker)
      .build();

    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
    this.sendMoneyAggregator = new SendMoneyAggregator(
      executor, streamConnectionMock, streamCodecContextMock, linkMock, congestionControllerMock,
      streamEncryptionServiceMock, Duration.ofMillis(10L), request
    );

    setSoldierOnBooleans(false, false, true);
    when(streamConnectionMock.nextSequence()).thenReturn(UnsignedLong.ONE);
    sendMoneyAggregator.send().get();
    verify(tracker, times(1)).auth(prepare);
    verify(tracker, times(0)).commit(any(), any());
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
      sampleRejectPacket(T00_INTERNAL_ERROR), defaultPrepareAmounts, new AtomicInteger(),
      congestionControllerMock);
  }

  @Test
  public void handleRejectHatesNullStreamPacket() {
    expectedException.expect(NullPointerException.class);
    sendMoneyAggregator.handleReject(samplePreparePacket(), null,
      sampleRejectPacket(T00_INTERNAL_ERROR), defaultPrepareAmounts, new AtomicInteger(),
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
    sendMoneyAggregator
      .handleReject(null, sampleStreamPacket(), sampleRejectPacket(T00_INTERNAL_ERROR),
        null, null, congestionControllerMock);
  }

  @Test
  public void handleRejectHatesNullCongestionController() {
    expectedException.expect(NullPointerException.class);
    sendMoneyAggregator.handleReject(null, sampleStreamPacket(),
      sampleRejectPacket(T00_INTERNAL_ERROR), defaultPrepareAmounts, new AtomicInteger(),
      null);
  }

  @Test
  public void handleRejectHatesNullPrepareAmounts() {
    expectedException.expect(NullPointerException.class);
    sendMoneyAggregator.handleReject(null, sampleStreamPacket(),
      sampleRejectPacket(T00_INTERNAL_ERROR), null, new AtomicInteger(),
      congestionControllerMock);
  }

  @Test
  public void handleReject() {
    UnsignedLong originalAmountToSend = paymentTracker.getOriginalAmountLeft();
    AtomicInteger numReject = new AtomicInteger(0);
    InterledgerPreparePacket prepare = samplePreparePacket();
    InterledgerRejectPacket reject = sampleRejectPacket(T00_INTERNAL_ERROR);
    sendMoneyAggregator.handleReject(prepare, sampleStreamPacket(),
      reject, defaultPrepareAmounts, numReject, congestionControllerMock);
    assertThat(numReject.get()).isEqualTo(1);
    assertThat(paymentTracker.getOriginalAmountLeft()).isEqualTo(originalAmountToSend.plus(prepare.getAmount()));
    verify(congestionControllerMock, times(1)).onReject(UnsignedLong.ONE, reject);
  }

  @Test
  public void handleRejectWithVariousErrorCodes() {
    // F Errors
    handleRejectTestHelper(F00_BAD_REQUEST, true);
    handleRejectTestHelper(InterledgerErrorCode.F01_INVALID_PACKET, true);
    handleRejectTestHelper(InterledgerErrorCode.F02_UNREACHABLE, true);
    handleRejectTestHelper(InterledgerErrorCode.F03_INVALID_AMOUNT, true);
    handleRejectTestHelper(InterledgerErrorCode.F04_INSUFFICIENT_DST_AMOUNT, true);
    handleRejectTestHelper(InterledgerErrorCode.F05_WRONG_CONDITION, true);
    handleRejectTestHelper(InterledgerErrorCode.F06_UNEXPECTED_PAYMENT, true);
    handleRejectTestHelper(InterledgerErrorCode.F07_CANNOT_RECEIVE, true);
    handleRejectTestHelper(InterledgerErrorCode.F08_AMOUNT_TOO_LARGE, false);
    handleRejectTestHelper(InterledgerErrorCode.F99_APPLICATION_ERROR, false);

    // T Errors
    handleRejectTestHelper(T00_INTERNAL_ERROR, false);
    handleRejectTestHelper(InterledgerErrorCode.T01_PEER_UNREACHABLE, false);
    handleRejectTestHelper(InterledgerErrorCode.T02_PEER_BUSY, false);
    handleRejectTestHelper(InterledgerErrorCode.T03_CONNECTOR_BUSY, false);
    handleRejectTestHelper(InterledgerErrorCode.T04_INSUFFICIENT_LIQUIDITY, false);
    handleRejectTestHelper(InterledgerErrorCode.T05_RATE_LIMITED, false);
    handleRejectTestHelper(InterledgerErrorCode.T99_APPLICATION_ERROR, false);

    // R Errors
    handleRejectTestHelper(InterledgerErrorCode.R00_TRANSFER_TIMED_OUT, true);
    handleRejectTestHelper(InterledgerErrorCode.R01_INSUFFICIENT_SOURCE_AMOUNT, true);
    handleRejectTestHelper(InterledgerErrorCode.R02_INSUFFICIENT_TIMEOUT, true);
    handleRejectTestHelper(InterledgerErrorCode.R99_APPLICATION_ERROR, true);
  }

  private void handleRejectTestHelper(
    final InterledgerErrorCode errorCodeToTest, final boolean expectedUnrecoverableState
  ) {
    Objects.requireNonNull(errorCodeToTest);

    ///////
    // Test Initialization
    sendMoneyAggregator.setUnrecoverableErrorEncountered(false);
    UnsignedLong originalAmountToSend = paymentTracker.getOriginalAmountLeft();
    AtomicInteger numReject = new AtomicInteger(0);
    InterledgerPreparePacket prepare = samplePreparePacket();
    InterledgerRejectPacket reject = sampleRejectPacket(errorCodeToTest);

    ///////
    // Do test
    sendMoneyAggregator.handleReject(
      prepare, sampleStreamPacket(), reject, defaultPrepareAmounts, numReject, congestionControllerMock
    );

    ///////
    // Assertions
    assertThat(numReject.get()).isEqualTo(1);
    assertThat(paymentTracker.getOriginalAmountLeft()).isEqualTo(originalAmountToSend.plus(prepare.getAmount()));
    verify(congestionControllerMock, times(1)).onReject(UnsignedLong.ONE, reject);
    assertThat(sendMoneyAggregator.isUnrecoverableErrorEncountered()).isEqualTo(expectedUnrecoverableState);
  }

  @Test
  public void testCheckForAndTriggerUnrecoverableError() {
    // F Errors
    testCheckForAndTriggerUnrecoverableErrorHelper(F00_BAD_REQUEST, true);
    testCheckForAndTriggerUnrecoverableErrorHelper(F01_INVALID_PACKET, true);
    testCheckForAndTriggerUnrecoverableErrorHelper(F02_UNREACHABLE, true);
    testCheckForAndTriggerUnrecoverableErrorHelper(F03_INVALID_AMOUNT, true);
    testCheckForAndTriggerUnrecoverableErrorHelper(F04_INSUFFICIENT_DST_AMOUNT, true);
    testCheckForAndTriggerUnrecoverableErrorHelper(F05_WRONG_CONDITION, true);
    testCheckForAndTriggerUnrecoverableErrorHelper(F06_UNEXPECTED_PAYMENT, true);
    testCheckForAndTriggerUnrecoverableErrorHelper(F07_CANNOT_RECEIVE, true);
    testCheckForAndTriggerUnrecoverableErrorHelper(F08_AMOUNT_TOO_LARGE, false);
    testCheckForAndTriggerUnrecoverableErrorHelper(F99_APPLICATION_ERROR, false);

    // T Errors
    testCheckForAndTriggerUnrecoverableErrorHelper(T00_INTERNAL_ERROR, false);
    testCheckForAndTriggerUnrecoverableErrorHelper(T01_PEER_UNREACHABLE, false);
    testCheckForAndTriggerUnrecoverableErrorHelper(T02_PEER_BUSY, false);
    testCheckForAndTriggerUnrecoverableErrorHelper(T03_CONNECTOR_BUSY, false);
    testCheckForAndTriggerUnrecoverableErrorHelper(T04_INSUFFICIENT_LIQUIDITY, false);
    testCheckForAndTriggerUnrecoverableErrorHelper(T05_RATE_LIMITED, false);
    testCheckForAndTriggerUnrecoverableErrorHelper(T99_APPLICATION_ERROR, false);

    // R Errors
    testCheckForAndTriggerUnrecoverableErrorHelper(R00_TRANSFER_TIMED_OUT, true);
    testCheckForAndTriggerUnrecoverableErrorHelper(R01_INSUFFICIENT_SOURCE_AMOUNT, true);
    testCheckForAndTriggerUnrecoverableErrorHelper(R02_INSUFFICIENT_TIMEOUT, true);
    testCheckForAndTriggerUnrecoverableErrorHelper(R99_APPLICATION_ERROR, true);
  }

  private void testCheckForAndTriggerUnrecoverableErrorHelper(
    final InterledgerErrorCode errorCodeToTest, final boolean expectedUnrecoverableState
  ) {
    Objects.requireNonNull(errorCodeToTest);
    InterledgerRejectPacket reject = sampleRejectPacket(errorCodeToTest);
    sendMoneyAggregator.setUnrecoverableErrorEncountered(false);
    sendMoneyAggregator.checkForAndTriggerUnrecoverableError(reject);
    assertThat(sendMoneyAggregator.isUnrecoverableErrorEncountered()).isEqualTo(expectedUnrecoverableState);
  }

  @Test
  public void preflightCheckFlagsAsUnrecoverable() throws Exception {
    when(streamConnectionMock.nextSequence()).thenReturn(UnsignedLong.ONE);
    when(linkMock.sendPacket(any())).thenReturn(sampleRejectPacket(F00_BAD_REQUEST));
    StreamPacket streamPacket = StreamPacket.builder().from(sampleStreamPacket())
      .addFrames(StreamMoneyFrame.builder()
        .shares(UnsignedLong.ONE)
        .streamId(UnsignedLong.ONE)
        .build())
      .build();
    when(streamCodecContextMock.read(any(), any())).thenReturn(streamPacket);
    assertThat(sendMoneyAggregator.isUnrecoverableErrorEncountered()).isFalse();
    sendMoneyAggregator.preflightCheck();
    assertThat(sendMoneyAggregator.isUnrecoverableErrorEncountered()).isTrue();
  }

  @Test
  public void stopSendingWhenUnrecoverableErrorEncountered() throws Exception {
    SendMoneyRequest request = SendMoneyRequest.builder()
      .sharedSecret(sharedSecret)
      .sourceAddress(sourceAddress)
      .destinationAddress(destinationAddress)
      .amount(originalAmountToSend)
      .timeout(Optional.of(Duration.ofSeconds(60)))
      .denomination(Denominations.XRP_DROPS)
      .paymentTracker(
        new FixedSenderAmountPaymentTracker(UnsignedLong.valueOf(10L), new NoOpExchangeRateCalculator())
      )
      .build();

    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);

    this.sendMoneyAggregator = new SendMoneyAggregator(
      executor, streamConnectionMock, streamCodecContextMock, linkMock, congestionControllerMock,
      streamEncryptionServiceMock, Duration.ofMillis(10L), request
    );

    when(congestionControllerMock.hasInFlight()).thenAnswer(new Answer<Boolean>() {

      private final AtomicInteger requests = new AtomicInteger(0);

      @Override
      public Boolean answer(InvocationOnMock invocationOnMock) {
        return requests.incrementAndGet() <= 10;
      }

    });

    when(congestionControllerMock.getMaxAmount()).thenReturn(UnsignedLong.ONE);

    when(streamConnectionMock.nextSequence()).thenReturn(UnsignedLong.ONE);
    when(linkMock.sendPacket(any())).thenAnswer(new Answer<InterledgerResponsePacket>() {
      private final AtomicInteger invocations = new AtomicInteger(0);

      @Override
      public InterledgerResponsePacket answer(InvocationOnMock invocationOnMock) {
        if (invocations.incrementAndGet() <= 2) {
          return sampleFulfillPacket();
        }
        return sampleRejectPacket(F00_BAD_REQUEST);
      }
    });
    StreamPacket streamPacket = StreamPacket.builder()
      .prepareAmount(UnsignedLong.ONE)
      .sequence(UnsignedLong.ZERO)
      .interledgerPacketType(InterledgerPacketType.FULFILL)
      .addFrames(StreamMoneyFrame.builder()
        .shares(UnsignedLong.ONE)
        .streamId(UnsignedLong.ONE)
        .build())
      .build();
    when(streamCodecContextMock.read(any(), any())).thenReturn(streamPacket);
    assertThat(sendMoneyAggregator.isUnrecoverableErrorEncountered()).isFalse();
    SendMoneyResult result = sendMoneyAggregator.send().get();
    assertThat(sendMoneyAggregator.isUnrecoverableErrorEncountered()).isTrue();
    assertThat(result)
      .extracting("amountDelivered", "amountSent", "numFulfilledPackets",
        "successfulPayment")
      .containsExactly(UnsignedLong.ONE, UnsignedLong.ONE, 1, false);
    // depending on execution order, we may halt sooner than trying all 10 potential times
    assertThat(result.numRejectPackets()).isLessThanOrEqualTo(9);
    assertThat(result.amountLeftToSend()).isLessThanOrEqualTo(UnsignedLong.valueOf(9));
    verify(congestionControllerMock, atMost(11)).getMaxAmount();
    verify(linkMock, atMost(12)).sendPacket(any());
    assertStreamCloseFrameSent();
  }

  /**
   * Test that if there is a delay between when a packet is schedule and when the executor sends the packet, that the
   * prepare packet expiresAt is calculated just before sending (not before scheduling).
   */
  @Test
  public void packetExpiryIsComputedJustBeforeSending() throws InterruptedException {
    SendMoneyRequest request = SendMoneyRequest.builder()
      .sharedSecret(sharedSecret)
      .sourceAddress(sourceAddress)
      .destinationAddress(destinationAddress)
      .amount(originalAmountToSend)
      .timeout(Optional.of(Duration.ofSeconds(60)))
      .denomination(Denominations.XRP_DROPS)
      .paymentTracker(
        new FixedSenderAmountPaymentTracker(UnsignedLong.valueOf(10L), new NoOpExchangeRateCalculator()))
      .build();

    // use a SleepyExecutorService with a non-trivial sleep so that we can verify that expiresAt is calculated
    // AFTER the scheduler woke up to process the packet.
    long scheduleDelayMillis = 100;
    ExecutorService executor = new SleepyExecutorService(Executors.newFixedThreadPool(1), scheduleDelayMillis);
    Instant minExpectedExpiresAt = DateUtils.now().plusMillis(scheduleDelayMillis);

    this.sendMoneyAggregator = new SendMoneyAggregator(
      executor, streamConnectionMock, streamCodecContextMock, linkMock, congestionControllerMock,
      streamEncryptionServiceMock, Duration.ofMillis(10L), request
    );

    sendMoneyAggregator.schedule(
      new AtomicBoolean(false),
      this::samplePreparePacket,
      sampleStreamPacket(),
      PrepareAmounts.builder().amountToSend(UnsignedLong.ONE).minimumAmountToAccept(UnsignedLong.ONE).build()
    );
    Thread.sleep(scheduleDelayMillis);
    ArgumentCaptor<InterledgerPreparePacket> prepareCaptor = ArgumentCaptor.forClass(InterledgerPreparePacket.class);

    verify(linkMock).sendPacket(prepareCaptor.capture());

    Instant actualExpiresAt = prepareCaptor.getValue().getExpiresAt();
    assertThat(actualExpiresAt).isAfter(minExpectedExpiresAt);
  }

  /**
   * Helper method to set the soldierOn mock values for clearer test coverage.
   */
  private void setSoldierOnBooleans(
    final boolean moneyInFlight, final boolean streamConnectionClosed, final boolean moreToSend
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
      .expiresAt(DateUtils.now())
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
      .fulfillment(InterledgerFulfillment.of(new byte[32]))
      .build();
  }

  /**
   * Extracts all the stream frames from all the prepare packets sent on the link.
   *
   * @return A {@link List}.
   */
  private List<StreamFrame> getStreamFramesSent() {
    ArgumentCaptor<InterledgerPreparePacket> prepareCaptor = ArgumentCaptor.forClass(InterledgerPreparePacket.class);
    verify(linkMock, atLeastOnce()).sendPacket(prepareCaptor.capture());
    return prepareCaptor.getAllValues().stream()
      .map(InterledgerPreparePacket::typedData)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .filter($ -> $ instanceof StreamPacket)
      .map($ -> (StreamPacket) $)
      .flatMap(packet -> packet.frames().stream())
      .collect(Collectors.toList());
  }

  /**
   * Assert that only preflight and close frames were sent, as is the case when no other frames can/need to be sent.
   */
  private void assertOnlyPreflightAndClose() {
    // Expect 2 Link calls due to preflight check and stream close
    Mockito.verify(linkMock, times(2)).sendPacket(any());
    assertStreamCloseFrameSent();
  }

  private void assertStreamCloseFrameSent() {
    assertThat(getStreamFramesSent()).contains(closeFrame());
  }

  private StreamCloseFrame closeFrame() {
    return StreamCloseFrame.builder().streamId(UnsignedLong.ONE).errorCode(ErrorCodes.NoError).build();
  }

}