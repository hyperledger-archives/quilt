package org.interledger.stream.pay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.fluent.Ratio;
import org.interledger.fx.Denominations;
import org.interledger.link.Link;
import org.interledger.stream.StreamPacket;
import org.interledger.stream.connection.StreamConnection;
import org.interledger.stream.crypto.StreamPacketEncryptionService;
import org.interledger.stream.crypto.StreamSharedSecret;
import org.interledger.stream.frames.ErrorCodes;
import org.interledger.stream.model.AccountDetails;
import org.interledger.stream.pay.exceptions.StreamPayerException;
import org.interledger.stream.pay.filters.StreamPacketFilter;
import org.interledger.stream.pay.filters.chain.StreamPacketFilterChain;
import org.interledger.stream.pay.model.ModifiableStreamPacketRequest;
import org.interledger.stream.pay.model.PaymentOptions;
import org.interledger.stream.pay.model.PaymentReceipt;
import org.interledger.stream.pay.model.Quote;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.model.StreamPacketReply;
import org.interledger.stream.pay.model.StreamPacketRequest;
import org.interledger.stream.pay.trackers.AmountTracker;
import org.interledger.stream.pay.trackers.ExchangeRateTracker;
import org.interledger.stream.pay.trackers.PaymentSharedStateTracker;
import org.interledger.stream.pay.trackers.StatisticsTracker;

import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit tests for {@link RunLoop}.
 */
public class RunLoopTest {

  @Mock
  private Link<?> linkMock;

  @Mock
  private StreamPacketEncryptionService streamPacketEncryptionService;

  @Mock
  private PaymentSharedStateTracker paymentSharedStateTrackerMock;

  @Mock
  private StreamPacketFilterChain streamPacketFilterChainMock;

  @Mock
  private AmountTracker amountTrackerMock;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  /**
   * This test incorporates one filter that returns READY 10 times, one that returns WAIT 1 time, and then returns END.
   */
  @Test
  public void testFullRunLoopExit() {

    // Return Wait one time during the run....
    StreamPacketFilter filter2 = new StreamPacketFilter() {
      private final AtomicInteger numRuns = new AtomicInteger(10);

      @Override
      public SendState nextState(ModifiableStreamPacketRequest streamPacketRequest) {
        int value = numRuns.decrementAndGet();
        if (value == 0) {
          return SendState.End;
        } else {
          if (value == 8) {
            return SendState.Wait;
          } else {
            return SendState.Ready;
          }
        }
      }

      @Override
      public StreamPacketReply doFilter(StreamPacketRequest streamPacketRequest, StreamPacketFilterChain filterChain) {
        return mock(StreamPacketReply.class);
      }
    };

    StreamPacketFilter filter1 = new StreamPacketFilter() {
      @Override
      public SendState nextState(ModifiableStreamPacketRequest streamPacketRequest) {
        return filter2.nextState(streamPacketRequest);
      }

      @Override
      public StreamPacketReply doFilter(StreamPacketRequest streamPacketRequest, StreamPacketFilterChain filterChain) {
        return filter2.doFilter(streamPacketRequest, filterChain);
      }
    };

    RunLoop runLoop = new RunLoop(
      linkMock,
      Arrays.asList(filter1, filter2),
      streamPacketEncryptionService,
      paymentSharedStateTrackerMock,
      5
    ) {
      @Override
      StreamPacketFilterChain constructNewFilterChain() {
        return streamPacketFilterChainMock;
      }
    };

    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    when(streamPacketFilterChainMock.nextState(any())).thenAnswer(
      (Answer<SendState>) invocation -> filter1.nextState(request)
    );

    Quote quoteMock = this.doMocks();

    final PaymentReceipt receipt = runLoop.start(quoteMock).join(); // <-- Execute the test

    // Assertions...
    assertThat(receipt.originalQuote()).isEqualTo(quoteMock);
    assertThat(receipt.amountDeliveredInDestinationUnits()).isEqualTo(BigInteger.ONE);
    assertThat(receipt.amountSentInSendersUnits()).isEqualTo(BigInteger.ONE);

    verify(streamPacketFilterChainMock, times(10)).nextState(any());
    // This value is 8 because doFilter is skipped once for the WAIT state, and then once for the END state.
    verify(streamPacketFilterChainMock, times(8)).doFilter(any());
    verify(paymentSharedStateTrackerMock, times(3)).getAmountTracker();
    verify(paymentSharedStateTrackerMock, times(3)).getStatisticsTracker();
    verify(paymentSharedStateTrackerMock, times(2)).getExchangeRateTracker();
    verify(streamPacketEncryptionService).toEncrypted(Mockito.<StreamSharedSecret>any(), any());
    verify(linkMock).sendPacket(any());

    verifyNoMoreInteractions(streamPacketFilterChainMock);
    verifyNoMoreInteractions(paymentSharedStateTrackerMock);
    verifyNoMoreInteractions(streamPacketEncryptionService);
    verifyNoMoreInteractions(linkMock);
  }

  /**
   * This test runs two filters, one of which returns a payment error.
   */
  @Test
  public void testFullRunLoopWithPaymentError() {

    // Return Wait one time during the run....
    StreamPacketFilter filter2 = new StreamPacketFilter() {
      private final AtomicInteger numRuns = new AtomicInteger(2);

      @Override
      public SendState nextState(ModifiableStreamPacketRequest streamPacketRequest) {
        int value = numRuns.decrementAndGet();
        if (value > 0) {
          return SendState.Ready;
        } else {
          return SendState.ConnectorError;
        }
      }

      @Override
      public StreamPacketReply doFilter(StreamPacketRequest streamPacketRequest, StreamPacketFilterChain filterChain) {
        return mock(StreamPacketReply.class);
      }
    };

    StreamPacketFilter filter1 = new StreamPacketFilter() {
      @Override
      public SendState nextState(ModifiableStreamPacketRequest streamPacketRequest) {
        return filter2.nextState(streamPacketRequest);
      }

      @Override
      public StreamPacketReply doFilter(StreamPacketRequest streamPacketRequest, StreamPacketFilterChain filterChain) {
        return filter2.doFilter(streamPacketRequest, filterChain);
      }
    };

    RunLoop runLoop = new RunLoop(
      linkMock,
      Arrays.asList(filter1, filter2),
      streamPacketEncryptionService,
      paymentSharedStateTrackerMock,
      5
    ) {
      @Override
      StreamPacketFilterChain constructNewFilterChain() {
        return streamPacketFilterChainMock;
      }
    };

    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    when(streamPacketFilterChainMock.nextState(any())).thenAnswer(
      (Answer<SendState>) invocation -> filter1.nextState(request)
    );

    Quote quoteMock = this.doMocks();

    final PaymentReceipt receipt = runLoop.start(quoteMock).join(); // <-- Execute the test

    // Assertions...
    assertThat(receipt.originalQuote()).isEqualTo(quoteMock);
    assertThat(receipt.amountDeliveredInDestinationUnits()).isEqualTo(BigInteger.ONE);
    assertThat(receipt.amountSentInSendersUnits()).isEqualTo(BigInteger.ONE);

    verify(streamPacketFilterChainMock, times(2)).nextState(any());
    verify(streamPacketFilterChainMock, times(1)).doFilter(any());
    verify(paymentSharedStateTrackerMock, times(3)).getAmountTracker();
    verify(paymentSharedStateTrackerMock, times(3)).getStatisticsTracker();
    verify(paymentSharedStateTrackerMock, times(2)).getExchangeRateTracker();
    verify(streamPacketEncryptionService).toEncrypted(Mockito.<StreamSharedSecret>any(), any());
    verify(linkMock).sendPacket(any());

    verifyNoMoreInteractions(streamPacketFilterChainMock);
    verifyNoMoreInteractions(paymentSharedStateTrackerMock);
    verifyNoMoreInteractions(streamPacketEncryptionService);
    verifyNoMoreInteractions(linkMock);
  }

  @Test
  public void testFullRunWith200ms() {

    // Return Wait one time during the run....
    StreamPacketFilter filter2 = new StreamPacketFilter() {
      private final AtomicInteger numRuns = new AtomicInteger(10);

      @Override
      public SendState nextState(ModifiableStreamPacketRequest streamPacketRequest) {
        int value = numRuns.decrementAndGet();
        if (value == 0) {
          return SendState.End;
        } else {
          if (value == 8) {
            return SendState.Wait;
          } else {
            return SendState.Ready; // <-- This trigger the InterruptedException for 200ms sleep
          }
        }
      }

      @Override
      public StreamPacketReply doFilter(StreamPacketRequest streamPacketRequest, StreamPacketFilterChain filterChain) {
        return mock(StreamPacketReply.class);
      }
    };

    StreamPacketFilter filter1 = new StreamPacketFilter() {
      @Override
      public SendState nextState(ModifiableStreamPacketRequest streamPacketRequest) {
        return filter2.nextState(streamPacketRequest);
      }

      @Override
      public StreamPacketReply doFilter(StreamPacketRequest streamPacketRequest, StreamPacketFilterChain filterChain) {
        return filter2.doFilter(streamPacketRequest, filterChain);
      }
    };

    RunLoop runLoop = new RunLoop(
      linkMock,
      Arrays.asList(filter1, filter2),
      streamPacketEncryptionService,
      paymentSharedStateTrackerMock,
      200
    ) {
      @Override
      StreamPacketFilterChain constructNewFilterChain() {
        return streamPacketFilterChainMock;
      }
    };

    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    when(streamPacketFilterChainMock.nextState(any())).thenAnswer(
      (Answer<SendState>) invocation -> filter1.nextState(request)
    );

    Quote quoteMock = this.doMocks();

    final PaymentReceipt receipt = runLoop.start(quoteMock).join(); // <-- Execute the test

    // Assertions...
    assertThat(receipt.originalQuote()).isEqualTo(quoteMock);
    assertThat(receipt.amountDeliveredInDestinationUnits()).isEqualTo(BigInteger.ONE);
    assertThat(receipt.amountSentInSendersUnits()).isEqualTo(BigInteger.ONE);

    verify(streamPacketFilterChainMock, times(10)).nextState(any());
    // This value is 8 because doFilter is skipped once for the WAIT state, and then once for the END state.
    verify(streamPacketFilterChainMock, times(8)).doFilter(any());
    verify(paymentSharedStateTrackerMock, times(3)).getAmountTracker();
    verify(paymentSharedStateTrackerMock, times(3)).getStatisticsTracker();
    verify(paymentSharedStateTrackerMock, times(2)).getExchangeRateTracker();
    verify(streamPacketEncryptionService).toEncrypted(Mockito.<StreamSharedSecret>any(), any());
    verify(linkMock).sendPacket(any());

    verifyNoMoreInteractions(streamPacketFilterChainMock);
    verifyNoMoreInteractions(paymentSharedStateTrackerMock);
    verifyNoMoreInteractions(streamPacketEncryptionService);
    verifyNoMoreInteractions(linkMock);
  }

  @Test
  public void testFullRunWithInterruptedExceptionAfterSleepReady() {
    StreamPacketFilter filter1 = new StreamPacketFilter() {
      private final AtomicInteger numRuns = new AtomicInteger(5);

      @Override
      public SendState nextState(ModifiableStreamPacketRequest streamPacketRequest) {
        int value = numRuns.decrementAndGet();
        if (value == 0) {
          return SendState.End;
        } else {
          return SendState.Ready;
        }
      }

      @Override
      public StreamPacketReply doFilter(StreamPacketRequest streamPacketRequest, StreamPacketFilterChain filterChain) {
        return mock(StreamPacketReply.class);
      }
    };

    RunLoop runLoop = new RunLoop(
      linkMock,
      Collections.singletonList(filter1),
      streamPacketEncryptionService,
      paymentSharedStateTrackerMock,
      200
    ) {
      @Override
      StreamPacketFilterChain constructNewFilterChain() {
        return streamPacketFilterChainMock;
      }

      @Override
      protected void sleep(int sleepTimeMs) throws InterruptedException {
        throw new InterruptedException();
      }
    };

    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    when(streamPacketFilterChainMock.nextState(any())).thenAnswer(
      (Answer<SendState>) invocation -> filter1.nextState(request)
    );

    Quote quoteMock = this.doMocks();

    final PaymentReceipt receipt = runLoop.start(quoteMock).join(); // <-- Execute the test

    // Assertions...
    assertThat(receipt.originalQuote()).isEqualTo(quoteMock);
    assertThat(receipt.amountDeliveredInDestinationUnits()).isEqualTo(BigInteger.ONE);
    assertThat(receipt.amountSentInSendersUnits()).isEqualTo(BigInteger.ONE);

    verify(streamPacketFilterChainMock, times(5)).nextState(any());
    // This value is 8 because doFilter is skipped once for the WAIT state, and then once for the END state.
    verify(streamPacketFilterChainMock, times(4)).doFilter(any());
    verify(paymentSharedStateTrackerMock, times(3)).getAmountTracker();
    verify(paymentSharedStateTrackerMock, times(3)).getStatisticsTracker();
    verify(paymentSharedStateTrackerMock, times(2)).getExchangeRateTracker();
    verify(streamPacketEncryptionService).toEncrypted(Mockito.<StreamSharedSecret>any(), any());
    verify(linkMock).sendPacket(any());

    verifyNoMoreInteractions(streamPacketFilterChainMock);
    verifyNoMoreInteractions(paymentSharedStateTrackerMock);
    verifyNoMoreInteractions(streamPacketEncryptionService);
    verifyNoMoreInteractions(linkMock);
  }

  /**
   * If a StreamPayerException is thrown from a nextState call, this the payment should still return a PaymentReceipt.
   * This test validates that.
   */
  @Test
  public void testFullRunWithStreamPayerExceptionInNextState() {
    StreamPacketFilter filter1 = new StreamPacketFilter() {
      @Override
      public SendState nextState(ModifiableStreamPacketRequest streamPacketRequest) {
        throw new StreamPayerException("foo", SendState.End);
      }

      @Override
      public StreamPacketReply doFilter(StreamPacketRequest streamPacketRequest, StreamPacketFilterChain filterChain) {
        return mock(StreamPacketReply.class);
      }
    };

    RunLoop runLoop = new RunLoop(
      linkMock,
      Collections.singletonList(filter1),
      streamPacketEncryptionService,
      paymentSharedStateTrackerMock,
      20
    );

    Quote quoteMock = this.doMocks();
    when(amountTrackerMock.getAmountSentInSourceUnits()).thenReturn(BigInteger.ZERO); // <-- Simulate 0 sent.

    final PaymentReceipt receipt = runLoop.start(quoteMock).join(); // <-- Execute the test

    // Assertions...
    assertThat(receipt.originalQuote()).isEqualTo(quoteMock);
    assertThat(receipt.amountDeliveredInDestinationUnits()).isEqualTo(BigInteger.ONE);
    assertThat(receipt.amountSentInSendersUnits()).isEqualTo(BigInteger.ZERO);

    verify(paymentSharedStateTrackerMock, times(3)).getAmountTracker();
    verify(paymentSharedStateTrackerMock, times(3)).getStatisticsTracker();
    verify(paymentSharedStateTrackerMock, times(2)).getExchangeRateTracker();
    verify(streamPacketEncryptionService).toEncrypted(Mockito.<StreamSharedSecret>any(), any());
    verify(linkMock).sendPacket(any());

    verifyNoMoreInteractions(streamPacketFilterChainMock);
    verifyNoMoreInteractions(paymentSharedStateTrackerMock);
    verifyNoMoreInteractions(streamPacketEncryptionService);
    verifyNoMoreInteractions(linkMock);
  }

  /**
   * If a RuntimeException is thrown from a nextState call, this the payment should still return a PaymentReceipt. This
   * test validates that.
   */
  @Test
  public void testFullRunWithRuntimeExceptionInNextState() {
    StreamPacketFilter filter1 = new StreamPacketFilter() {
      @Override
      public SendState nextState(ModifiableStreamPacketRequest streamPacketRequest) {
        throw new RuntimeException("foo");
      }

      @Override
      public StreamPacketReply doFilter(StreamPacketRequest streamPacketRequest, StreamPacketFilterChain filterChain) {
        return mock(StreamPacketReply.class);
      }
    };

    RunLoop runLoop = new RunLoop(
      linkMock,
      Collections.singletonList(filter1),
      streamPacketEncryptionService,
      paymentSharedStateTrackerMock,
      20
    );

    Quote quoteMock = this.doMocks();
    when(amountTrackerMock.getAmountSentInSourceUnits()).thenReturn(BigInteger.ZERO); // <-- Simulate 0 sent.

    final PaymentReceipt receipt = runLoop.start(quoteMock).join(); // <-- Execute the test

    // Assertions...
    assertThat(receipt.originalQuote()).isEqualTo(quoteMock);
    assertThat(receipt.amountDeliveredInDestinationUnits()).isEqualTo(BigInteger.ONE);
    assertThat(receipt.amountSentInSendersUnits()).isEqualTo(BigInteger.ZERO);

    verify(paymentSharedStateTrackerMock, times(3)).getAmountTracker();
    verify(paymentSharedStateTrackerMock, times(3)).getStatisticsTracker();
    verify(paymentSharedStateTrackerMock, times(2)).getExchangeRateTracker();
    verify(streamPacketEncryptionService).toEncrypted(Mockito.<StreamSharedSecret>any(), any());
    verify(linkMock).sendPacket(any());

    verifyNoMoreInteractions(streamPacketFilterChainMock);
    verifyNoMoreInteractions(paymentSharedStateTrackerMock);
    verifyNoMoreInteractions(streamPacketEncryptionService);
    verifyNoMoreInteractions(linkMock);
  }

  /**
   * If a RuntimeException is thrown from a doFilter, this the payment should still return a PaymentReceipt. This test
   * validates that.
   */
  @Test
  public void testFullRunWithRuntimeExceptionInDoFilter() {
    StreamPacketFilter filter1 = new StreamPacketFilter() {
      private final AtomicInteger numRuns = new AtomicInteger(5);

      @Override
      public SendState nextState(ModifiableStreamPacketRequest streamPacketRequest) {
        int value = numRuns.decrementAndGet();
        if (value == 0) {
          return SendState.End;
        } else {
          return SendState.Ready;
        }
      }

      @Override
      public StreamPacketReply doFilter(StreamPacketRequest streamPacketRequest, StreamPacketFilterChain filterChain) {
        throw new RuntimeException("foo");
      }
    };

    RunLoop runLoop = new RunLoop(
      linkMock,
      Collections.singletonList(filter1),
      streamPacketEncryptionService,
      paymentSharedStateTrackerMock,
      20
    );

    Quote quoteMock = this.doMocks();
    when(amountTrackerMock.getAmountSentInSourceUnits()).thenReturn(BigInteger.ZERO); // <-- Simulate 0 sent.

    final PaymentReceipt receipt = runLoop.start(quoteMock).join(); // <-- Execute the test

    // Assertions...
    assertThat(receipt.originalQuote()).isEqualTo(quoteMock);
    assertThat(receipt.amountDeliveredInDestinationUnits()).isEqualTo(BigInteger.ONE);
    assertThat(receipt.amountSentInSendersUnits()).isEqualTo(BigInteger.ZERO);

    verify(paymentSharedStateTrackerMock, times(3)).getAmountTracker();
    verify(paymentSharedStateTrackerMock, times(3)).getStatisticsTracker();
    verify(paymentSharedStateTrackerMock, times(2)).getExchangeRateTracker();
    verify(streamPacketEncryptionService).toEncrypted(Mockito.<StreamSharedSecret>any(), any());
    verify(linkMock).sendPacket(any());

    verifyNoMoreInteractions(streamPacketFilterChainMock);
    verifyNoMoreInteractions(paymentSharedStateTrackerMock);
    verifyNoMoreInteractions(streamPacketEncryptionService);
    verifyNoMoreInteractions(linkMock);
  }

  /**
   * If a StreamPayerException is thrown from a doFilter, this the payment should still return a PaymentReceipt. This
   * test validates that.
   */
  @Test
  public void testFullRunWithStreamPayerExceptionInDoFilter() {
    StreamPacketFilter filter1 = new StreamPacketFilter() {
      private final AtomicInteger numRuns = new AtomicInteger(5);

      @Override
      public SendState nextState(ModifiableStreamPacketRequest streamPacketRequest) {
        int value = numRuns.decrementAndGet();
        if (value == 0) {
          return SendState.End;
        } else {
          return SendState.Ready;
        }
      }

      @Override
      public StreamPacketReply doFilter(StreamPacketRequest streamPacketRequest, StreamPacketFilterChain filterChain) {
        throw new StreamPayerException("foo", SendState.QueryFailed);
      }
    };

    RunLoop runLoop = new RunLoop(
      linkMock,
      Collections.singletonList(filter1),
      streamPacketEncryptionService,
      paymentSharedStateTrackerMock,
      20
    );

    Quote quoteMock = this.doMocks();
    when(amountTrackerMock.getAmountSentInSourceUnits()).thenReturn(BigInteger.ZERO); // <-- Simulate 0 sent.

    final PaymentReceipt receipt = runLoop.start(quoteMock).join(); // <-- Execute the test

    // Assertions...
    assertThat(receipt.originalQuote()).isEqualTo(quoteMock);
    assertThat(receipt.amountDeliveredInDestinationUnits()).isEqualTo(BigInteger.ONE);
    assertThat(receipt.amountSentInSendersUnits()).isEqualTo(BigInteger.ZERO);

    verify(paymentSharedStateTrackerMock, times(3)).getAmountTracker();
    verify(paymentSharedStateTrackerMock, times(3)).getStatisticsTracker();
    verify(paymentSharedStateTrackerMock, times(2)).getExchangeRateTracker();
    verify(streamPacketEncryptionService).toEncrypted(Mockito.<StreamSharedSecret>any(), any());
    verify(linkMock).sendPacket(any());

    verifyNoMoreInteractions(streamPacketFilterChainMock);
    verifyNoMoreInteractions(paymentSharedStateTrackerMock);
    verifyNoMoreInteractions(streamPacketEncryptionService);
    verifyNoMoreInteractions(linkMock);
  }

  /**
   * If a sleep is interrupted, we keep processing the run-loop to completion. This test validates that.
   */
  @Test
  public void testFullRunWithInterruptedExceptionAfterSleepWait() {
    StreamPacketFilter filter1 = new StreamPacketFilter() {
      private final AtomicInteger numRuns = new AtomicInteger(5);

      @Override
      public SendState nextState(ModifiableStreamPacketRequest streamPacketRequest) {
        int value = numRuns.decrementAndGet();
        if (value == 0) {
          return SendState.End;
        } else {
          return SendState.Wait; // <-- This trigger the InterruptedException for 50ms sleep
        }
      }

      @Override
      public StreamPacketReply doFilter(StreamPacketRequest streamPacketRequest, StreamPacketFilterChain filterChain) {
        return mock(StreamPacketReply.class);
      }
    };

    RunLoop runLoop = new RunLoop(
      linkMock,
      Collections.singletonList(filter1),
      streamPacketEncryptionService,
      paymentSharedStateTrackerMock,
      200
    ) {
      @Override
      StreamPacketFilterChain constructNewFilterChain() {
        return streamPacketFilterChainMock;
      }

      @Override
      protected void sleep(int sleepTimeMs) throws InterruptedException {
        throw new InterruptedException();
      }
    };

    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    when(streamPacketFilterChainMock.nextState(any())).thenAnswer(
      (Answer<SendState>) invocation -> filter1.nextState(request)
    );

    Quote quoteMock = this.doMocks();

    final PaymentReceipt receipt = runLoop.start(quoteMock).join(); // <-- Execute the test

    // Assertions...
    assertThat(receipt.originalQuote()).isEqualTo(quoteMock);
    assertThat(receipt.amountDeliveredInDestinationUnits()).isEqualTo(BigInteger.ONE);
    assertThat(receipt.amountSentInSendersUnits()).isEqualTo(BigInteger.ONE);

    verify(streamPacketFilterChainMock, times(5)).nextState(any());
    verify(paymentSharedStateTrackerMock, times(3)).getAmountTracker();
    verify(paymentSharedStateTrackerMock, times(3)).getStatisticsTracker();
    verify(paymentSharedStateTrackerMock, times(2)).getExchangeRateTracker();
    verify(streamPacketEncryptionService).toEncrypted(Mockito.<StreamSharedSecret>any(), any());
    verify(linkMock).sendPacket(any());

    verifyNoMoreInteractions(streamPacketFilterChainMock);
    verifyNoMoreInteractions(paymentSharedStateTrackerMock);
    verifyNoMoreInteractions(streamPacketEncryptionService);
    verifyNoMoreInteractions(linkMock);
  }

  @Test
  public void shouldCloseConnection() {
    RunLoop runLoop = new RunLoop(
      linkMock,
      Collections.emptyList(),
      streamPacketEncryptionService,
      paymentSharedStateTrackerMock
    );

    assertThat(runLoop.shouldCloseConnection(SendState.QueryFailed)).isTrue();
    assertThat(runLoop.shouldCloseConnection(SendState.Ready)).isFalse();
    assertThat(runLoop.shouldCloseConnection(SendState.Wait)).isFalse();
    assertThat(runLoop.shouldCloseConnection(SendState.End)).isTrue();
    assertThat(runLoop.shouldCloseConnection(SendState.ClosedByRecipient)).isFalse();
  }

  @Test
  public void shouldCloseConnectionWithNoError() {
    StreamConnection streamConnectionMock = mock(StreamConnection.class);
    when(streamConnectionMock.nextSequence()).thenReturn(UnsignedLong.ONE);
    when(streamConnectionMock.getDestinationAddress()).thenReturn(InterledgerAddress.of("example.foo"));
    when(streamConnectionMock.getStreamSharedSecret()).thenReturn(mock(StreamSharedSecret.class));

    RunLoop runLoop = new RunLoop(
      linkMock,
      Collections.emptyList(),
      streamPacketEncryptionService,
      paymentSharedStateTrackerMock
    );

    runLoop.closeConnection(streamConnectionMock, ErrorCodes.NoError);
  }

  @Test
  public void shouldCloseConnectionWithError() {
    RunLoop runLoop = new RunLoop(
      linkMock,
      Collections.emptyList(),
      streamPacketEncryptionService,
      paymentSharedStateTrackerMock
    );

    StreamPacket streamPacketMock = mock(StreamPacket.class);
    when(streamPacketEncryptionService.fromEncrypted(Mockito.<StreamSharedSecret>any(), any()))
      .thenReturn(streamPacketMock);
    when(streamPacketEncryptionService.toEncrypted(Mockito.<StreamSharedSecret>any(), any())).thenReturn(new byte[0]);

    StreamConnection streamConnectionMock = mock(StreamConnection.class);
    when(streamConnectionMock.nextSequence()).thenReturn(UnsignedLong.ONE);
    when(streamConnectionMock.getDestinationAddress()).thenReturn(InterledgerAddress.of("example.foo"));
    when(streamConnectionMock.getStreamSharedSecret()).thenReturn(mock(StreamSharedSecret.class));
    doThrow(new RuntimeException()).when(linkMock).sendPacket(any());

    try {
      runLoop.closeConnection(streamConnectionMock, ErrorCodes.NoError);
    } catch (RuntimeException e) {
      fail(); // <-- No Exception should be thrown...
    }
  }

  ///////////////////
  // Private Helpers
  ///////////////////

  private Quote doMocks() {
    Quote quoteMock = mock(Quote.class);

    AccountDetails sourceAccountMock = mock(AccountDetails.class);
    when(sourceAccountMock.denomination()).thenReturn(Optional.of(Denominations.USD));
    when(quoteMock.sourceAccount()).thenReturn(sourceAccountMock);

    PaymentOptions paymentOptionsMock = mock(PaymentOptions.class);
    when(paymentOptionsMock.amountToSend()).thenReturn(BigDecimal.ONE);
    when(quoteMock.paymentOptions()).thenReturn(paymentOptionsMock);

    StreamConnection streamConnectionMock = mock(StreamConnection.class);
    when(quoteMock.streamConnection()).thenReturn(streamConnectionMock);
    when(streamConnectionMock.nextSequence()).thenReturn(UnsignedLong.ONE);
    when(streamConnectionMock.getDestinationAddress()).thenReturn(InterledgerAddress.of("example.foo"));
    when(streamConnectionMock.getStreamSharedSecret()).thenReturn(mock(StreamSharedSecret.class));

    when(paymentSharedStateTrackerMock.getAmountTracker()).thenReturn(amountTrackerMock);
    when(amountTrackerMock.getAmountDeliveredInDestinationUnits()).thenReturn(BigInteger.ONE);
    when(amountTrackerMock.getAmountSentInSourceUnits()).thenReturn(BigInteger.ONE);

    StatisticsTracker statisticsTrackerMock = mock(StatisticsTracker.class);
    when(paymentSharedStateTrackerMock.getStatisticsTracker()).thenReturn(statisticsTrackerMock);
    when(statisticsTrackerMock.getNumRejects()).thenReturn(1);
    when(statisticsTrackerMock.getNumFulfills()).thenReturn(1);
    when(statisticsTrackerMock.getTotalPacketResponses()).thenReturn(1);
    when(statisticsTrackerMock.getPaymentStartInstant()).thenReturn(Instant.now());

    ExchangeRateTracker exchangeRateTrackerMock = mock(ExchangeRateTracker.class);
    when(paymentSharedStateTrackerMock.getExchangeRateTracker()).thenReturn(exchangeRateTrackerMock);
    when(exchangeRateTrackerMock.getLowerBoundRate()).thenReturn(Ratio.ONE);
    when(exchangeRateTrackerMock.getUpperBoundRate()).thenReturn(Ratio.ONE);

    StreamPacket streamPacketMock = mock(StreamPacket.class);
    when(streamPacketEncryptionService.fromEncrypted(Mockito.<StreamSharedSecret>any(), any()))
      .thenReturn(streamPacketMock);
    when(streamPacketEncryptionService.toEncrypted(Mockito.<StreamSharedSecret>any(), any())).thenReturn(new byte[0]);

    return quoteMock;
  }
}