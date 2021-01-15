package org.interledger.stream.pay.filters.chain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.interledger.codecs.stream.StreamCodecContextFactory;
import org.interledger.core.AmountTooLargeErrorData;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.link.Link;
import org.interledger.stream.crypto.AesGcmStreamSharedSecretCrypto;
import org.interledger.core.SharedSecret;;
import org.interledger.stream.crypto.StreamPacketEncryptionService;
import org.interledger.stream.connection.StreamConnection;
import org.interledger.stream.pay.exceptions.StreamPayerException;
import org.interledger.stream.pay.filters.StreamPacketFilter;
import org.interledger.stream.pay.model.ModifiableStreamPacketRequest;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.model.StreamPacketReply;
import org.interledger.stream.pay.model.StreamPacketRequest;
import org.interledger.stream.pay.trackers.PaymentSharedStateTracker;

import com.google.common.primitives.UnsignedLong;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testcontainers.shaded.com.google.common.io.BaseEncoding;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit test for {@link DefaultStreamPacketFilterChain}.
 */
@SuppressWarnings("OptionalGetWithoutIsPresent")
public class DefaultStreamPacketFilterChainTest {

  @Mock
  private Link<?> linkMock;

  @Mock
  private PaymentSharedStateTracker paymentSharedStateTrackerMock;

  @Mock
  private List<StreamPacketFilter> streamPacketFiltersMock;

  private List<StreamPacketFilter> streamPacketFilters;
  private StreamPacketFilterChain streamPacketFilterChain;

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    StreamPacketEncryptionService streamPacketEncryptionService = new StreamPacketEncryptionService(
      StreamCodecContextFactory.oer(),
      new AesGcmStreamSharedSecretCrypto()
    );

    this.streamPacketFilters = Lists.newArrayList();

    this.streamPacketFilterChain = new DefaultStreamPacketFilterChain(
      streamPacketFilters,
      linkMock,
      streamPacketEncryptionService,
      paymentSharedStateTrackerMock
    );
  }

  ////////////
  // nextState
  ////////////

  @Test
  public void nextStateWithNoFilters() {
    assertThat(this.streamPacketFiltersMock.size()).isEqualTo(0);

    streamPacketFilterChain.nextState(ModifiableStreamPacketRequest.create());

    verifyNoMoreInteractions(linkMock);
    verifyNoMoreInteractions(paymentSharedStateTrackerMock);
  }

  @Test
  public void nextStateWithMultipleFilters() {
    final TestableStreamPacketFilter filter1 = new TestableStreamPacketFilter();
    this.streamPacketFilters.add(filter1);

    final TestableStreamPacketFilter filter2 = new TestableStreamPacketFilter();
    this.streamPacketFilters.add(filter2);

    final TestableStreamPacketFilter filter3 = new TestableStreamPacketFilter();
    this.streamPacketFilters.add(filter3);

    assertThat(this.streamPacketFilters.size()).isEqualTo(3);

    SendState result = streamPacketFilterChain.nextState(ModifiableStreamPacketRequest.create());
    assertThat(result).isEqualTo(SendState.Ready);

    // Each filter should only be called once...
    assertThat(filter1.getNumCalls().get()).isEqualTo(1);
    assertThat(filter2.getNumCalls().get()).isEqualTo(1);
    assertThat(filter3.getNumCalls().get()).isEqualTo(1);

    verifyNoMoreInteractions(linkMock);
    verifyNoMoreInteractions(paymentSharedStateTrackerMock);
  }

  @Test
  public void nextStateWithExceptionInFirstFilter() {
    final TestableStreamPacketFilter filter1 = new TestableStreamPacketFilter(true, false);
    this.streamPacketFilters.add(filter1);

    final TestableStreamPacketFilter filter2 = new TestableStreamPacketFilter();
    this.streamPacketFilters.add(filter2);

    final TestableStreamPacketFilter filter3 = new TestableStreamPacketFilter();
    this.streamPacketFilters.add(filter3);

    assertThat(this.streamPacketFilters.size()).isEqualTo(3);

    try {
      streamPacketFilterChain.nextState(ModifiableStreamPacketRequest.create());
      fail("should have thrown");
    } catch (StreamPayerException e) {
      // Only the first filter should get called.
      assertThat(filter1.getNumCalls().get()).isEqualTo(1);
      assertThat(filter2.getNumCalls().get()).isEqualTo(0);
      assertThat(filter3.getNumCalls().get()).isEqualTo(0);

      verifyNoMoreInteractions(linkMock);
      verifyNoMoreInteractions(paymentSharedStateTrackerMock);

      assertThat(e.getSendState()).isEqualTo(SendState.ConnectorError);
    }
  }

  @Test
  public void nextStateWithExceptionInMiddleFilter() {
    final TestableStreamPacketFilter filter1 = new TestableStreamPacketFilter();
    this.streamPacketFilters.add(filter1);

    final TestableStreamPacketFilter filter2 = new TestableStreamPacketFilter(true, false);
    this.streamPacketFilters.add(filter2);

    final TestableStreamPacketFilter filter3 = new TestableStreamPacketFilter();
    this.streamPacketFilters.add(filter3);

    assertThat(this.streamPacketFilters.size()).isEqualTo(3);

    try {
      streamPacketFilterChain.nextState(ModifiableStreamPacketRequest.create());
      fail("should have thrown");
    } catch (StreamPayerException e) {
      // Only the first filter should get called.
      assertThat(filter1.getNumCalls().get()).isEqualTo(1);
      assertThat(filter2.getNumCalls().get()).isEqualTo(1);
      assertThat(filter3.getNumCalls().get()).isEqualTo(0);

      verifyNoMoreInteractions(linkMock);
      verifyNoMoreInteractions(paymentSharedStateTrackerMock);

      assertThat(e.getSendState()).isEqualTo(SendState.ConnectorError);
    }
  }

  @Test
  public void nextStateWithExceptionInLastFilter() {
    final TestableStreamPacketFilter filter1 = new TestableStreamPacketFilter();
    this.streamPacketFilters.add(filter1);

    final TestableStreamPacketFilter filter2 = new TestableStreamPacketFilter();
    this.streamPacketFilters.add(filter2);

    final TestableStreamPacketFilter filter3 = new TestableStreamPacketFilter(true, false);
    this.streamPacketFilters.add(filter3);

    assertThat(this.streamPacketFilters.size()).isEqualTo(3);

    try {
      streamPacketFilterChain.nextState(ModifiableStreamPacketRequest.create());
      fail("should have thrown");
    } catch (StreamPayerException e) {
      // Only the first filter should get called.
      assertThat(filter1.getNumCalls().get()).isEqualTo(1);
      assertThat(filter2.getNumCalls().get()).isEqualTo(1);
      assertThat(filter3.getNumCalls().get()).isEqualTo(1);

      verifyNoMoreInteractions(linkMock);
      verifyNoMoreInteractions(paymentSharedStateTrackerMock);

      assertThat(e.getSendState()).isEqualTo(SendState.ConnectorError);
    }
  }

  @Test
  public void nextStateWithNullPointerExceptionInFirstFilter() {
    final TestableStreamPacketFilter filter1 = new TestableStreamPacketFilter() {
      @Override
      public SendState nextState(ModifiableStreamPacketRequest streamPacketRequest) {
        super.nextState(streamPacketRequest);
        throw new NullPointerException(); // <-- This filter throws an exception that's not a SPE.
      }
    };
    this.streamPacketFilters.add(filter1);

    final TestableStreamPacketFilter filter2 = new TestableStreamPacketFilter();
    this.streamPacketFilters.add(filter2);

    final TestableStreamPacketFilter filter3 = new TestableStreamPacketFilter(true, false);
    this.streamPacketFilters.add(filter3);

    assertThat(this.streamPacketFilters.size()).isEqualTo(3);

    try {
      streamPacketFilterChain.nextState(ModifiableStreamPacketRequest.create());
      fail("should have thrown");
    } catch (StreamPayerException e) {
      // Only the first filter should get called.
      assertThat(filter1.getNumCalls().get()).isEqualTo(1);
      assertThat(filter2.getNumCalls().get()).isEqualTo(0);
      assertThat(filter3.getNumCalls().get()).isEqualTo(0);

      verifyNoMoreInteractions(linkMock);
      verifyNoMoreInteractions(paymentSharedStateTrackerMock);

      assertThat(e.getSendState()).isEqualTo(SendState.End);
    }
  }

  @Test
  public void nextStateWithPrematureEndInFirstFilter() {
    final TestableStreamPacketFilter filter1 = new TestableStreamPacketFilter() {
      @Override
      public SendState nextState(ModifiableStreamPacketRequest streamPacketRequest) {
        super.nextState(streamPacketRequest);
        return SendState.End; // <-- This filter ends prematurely.
      }
    };
    this.streamPacketFilters.add(filter1);

    final TestableStreamPacketFilter filter2 = new TestableStreamPacketFilter();
    this.streamPacketFilters.add(filter2);

    final TestableStreamPacketFilter filter3 = new TestableStreamPacketFilter(true, false);
    this.streamPacketFilters.add(filter3);

    assertThat(this.streamPacketFilters.size()).isEqualTo(3);

    assertThat(streamPacketFilterChain.nextState(ModifiableStreamPacketRequest.create())).isEqualTo(SendState.End);
    // Only the first filter should get called.
    assertThat(filter1.getNumCalls().get()).isEqualTo(1);
    assertThat(filter2.getNumCalls().get()).isEqualTo(0);
    assertThat(filter3.getNumCalls().get()).isEqualTo(0);

    verifyNoMoreInteractions(linkMock);
    verifyNoMoreInteractions(paymentSharedStateTrackerMock);
  }

  ////////////
  // doFilter
  ////////////

  @Test
  public void doFilterWithNoFilters() {
    assertThat(this.streamPacketFiltersMock.size()).isEqualTo(0);

    streamPacketFilterChain.doFilter(StreamPacketRequest.builder()
      .sendState(SendState.Ready)
      .build());

    verify(paymentSharedStateTrackerMock).getStreamConnection();
    verifyNoMoreInteractions(linkMock);
    verifyNoMoreInteractions(paymentSharedStateTrackerMock);
  }

  @Test
  public void doFilterWithMultipleFilters() {
    final TestableStreamPacketFilter filter1 = new TestableStreamPacketFilter();
    this.streamPacketFilters.add(filter1);

    final TestableStreamPacketFilter filter2 = new TestableStreamPacketFilter();
    this.streamPacketFilters.add(filter2);

    final TestableStreamPacketFilter filter3 = new TestableStreamPacketFilter();
    this.streamPacketFilters.add(filter3);

    assertThat(this.streamPacketFilters.size()).isEqualTo(3);

    final StreamPacketRequest streamRequest = StreamPacketRequest.builder()
      .sendState(SendState.Ready)
      .build();
    streamPacketFilterChain.doFilter(streamRequest);

    // Each filter should only be called once...

    assertThat(filter1.getNumCalls().get()).isEqualTo(1);
    assertThat(filter2.getNumCalls().get()).isEqualTo(1);
    assertThat(filter3.getNumCalls().get()).isEqualTo(1);
    verify(paymentSharedStateTrackerMock).getStreamConnection();

    verifyNoMoreInteractions(linkMock);
    verifyNoMoreInteractions(paymentSharedStateTrackerMock);
  }

  @Test
  public void filterPacketWithExceptionInFirstFilter() {
    final TestableStreamPacketFilter filter1 = new TestableStreamPacketFilter(false, true);
    this.streamPacketFilters.add(filter1);

    final TestableStreamPacketFilter filter2 = new TestableStreamPacketFilter();
    this.streamPacketFilters.add(filter2);

    final TestableStreamPacketFilter filter3 = new TestableStreamPacketFilter();
    this.streamPacketFilters.add(filter3);

    assertThat(this.streamPacketFilters.size()).isEqualTo(3);

    final StreamPacketRequest streamRequest = StreamPacketRequest.builder()
      .sendState(SendState.Ready)
      .build();

    StreamPacketReply result = streamPacketFilterChain.doFilter(streamRequest);
    assertThat(result.exception()).isPresent();
    assertThat(((StreamPayerException) result.exception().get()).getSendState())
      .isEqualTo(SendState.InsufficientExchangeRate);

    // Only the first filter should get called.
    assertThat(filter1.getNumCalls().get()).isEqualTo(1);
    assertThat(filter2.getNumCalls().get()).isEqualTo(0);
    assertThat(filter3.getNumCalls().get()).isEqualTo(0);

    verifyNoMoreInteractions(linkMock);
    verifyNoMoreInteractions(paymentSharedStateTrackerMock);
  }

  @Test
  public void filterPacketWithExceptionInMiddleFilter() {
    final TestableStreamPacketFilter filter1 = new TestableStreamPacketFilter();
    this.streamPacketFilters.add(filter1);

    final TestableStreamPacketFilter filter2 = new TestableStreamPacketFilter(false, true);
    this.streamPacketFilters.add(filter2);

    final TestableStreamPacketFilter filter3 = new TestableStreamPacketFilter();
    this.streamPacketFilters.add(filter3);

    assertThat(this.streamPacketFilters.size()).isEqualTo(3);

    final StreamPacketRequest streamRequest = StreamPacketRequest.builder()
      .sendState(SendState.Ready)
      .build();

    StreamPacketReply result = streamPacketFilterChain.doFilter(streamRequest);
    assertThat(result.exception()).isPresent();
    assertThat(((StreamPayerException) result.exception().get()).getSendState())
      .isEqualTo(SendState.InsufficientExchangeRate);

    // Only the first filter should get called.
    assertThat(filter1.getNumCalls().get()).isEqualTo(1);
    assertThat(filter2.getNumCalls().get()).isEqualTo(1);
    assertThat(filter3.getNumCalls().get()).isEqualTo(0);

    verifyNoMoreInteractions(linkMock);
    verifyNoMoreInteractions(paymentSharedStateTrackerMock);
  }

  @Test
  public void filterPacketWithExceptionInLastFilter() {
    final TestableStreamPacketFilter filter1 = new TestableStreamPacketFilter();
    this.streamPacketFilters.add(filter1);

    final TestableStreamPacketFilter filter2 = new TestableStreamPacketFilter();
    this.streamPacketFilters.add(filter2);

    final TestableStreamPacketFilter filter3 = new TestableStreamPacketFilter(false, true);
    this.streamPacketFilters.add(filter3);

    assertThat(this.streamPacketFilters.size()).isEqualTo(3);

    final StreamPacketRequest streamRequest = StreamPacketRequest.builder()
      .sendState(SendState.Ready)
      .build();

    StreamPacketReply result = streamPacketFilterChain.doFilter(streamRequest);
    assertThat(result.exception()).isPresent();
    assertThat(((StreamPayerException) result.exception().get()).getSendState())
      .isEqualTo(SendState.InsufficientExchangeRate);

    // Only the first filter should get called.
    assertThat(filter1.getNumCalls().get()).isEqualTo(1);
    assertThat(filter2.getNumCalls().get()).isEqualTo(1);
    assertThat(filter3.getNumCalls().get()).isEqualTo(1);

    verifyNoMoreInteractions(linkMock);
    verifyNoMoreInteractions(paymentSharedStateTrackerMock);
  }

  @Test
  public void doFilterWithSendStateEnd() {
    InterledgerPreparePacket preparePacket = mock(InterledgerPreparePacket.class);
    StreamPacketReply result = streamPacketFilterChain.doFilter(StreamPacketRequest.builder()
      .interledgerPreparePacket(Optional.of(preparePacket))
      .sendState(SendState.End)
      .build());

    assertThat(result.exception()).isEmpty();
    assertThat(result.interledgerPreparePacket().get()).isEqualTo(preparePacket);
    verifyNoMoreInteractions(linkMock);
    verifyNoMoreInteractions(paymentSharedStateTrackerMock);
  }

  @Test
  public void doFilterWithSendStateWait() {
    InterledgerPreparePacket preparePacket = mock(InterledgerPreparePacket.class);
    StreamPacketReply result = streamPacketFilterChain.doFilter(StreamPacketRequest.builder()
      .interledgerPreparePacket(Optional.of(preparePacket))
      .sendState(SendState.Wait)
      .build());

    assertThat(result.exception()).isEmpty();
    assertThat(result.interledgerPreparePacket().get()).isEqualTo(preparePacket);
    verifyNoMoreInteractions(linkMock);
    verifyNoMoreInteractions(paymentSharedStateTrackerMock);
  }

  @Test
  public void doFilterWithSendStatePaymentError() {
    InterledgerPreparePacket preparePacket = mock(InterledgerPreparePacket.class);
    StreamPacketReply result = streamPacketFilterChain.doFilter(StreamPacketRequest.builder()
      .interledgerPreparePacket(Optional.of(preparePacket))
      .sendState(SendState.ConnectorError)
      .build());

    assertThat(result.exception()).isEmpty();
    assertThat(result.interledgerPreparePacket().get()).isEqualTo(preparePacket);
    verifyNoMoreInteractions(linkMock);
    verifyNoMoreInteractions(paymentSharedStateTrackerMock);
  }

  @Test
  public void doFilterWithSendStateReadyFulfillable() {
    StreamConnection streamConnectionMock = mock(StreamConnection.class);
    when(streamConnectionMock.getStreamSharedSecret()).thenReturn(SharedSecret.of(new byte[32]));
    when(streamConnectionMock.getDestinationAddress()).thenReturn(InterledgerAddress.of("private.test"));
    when(paymentSharedStateTrackerMock.getStreamConnection()).thenReturn(streamConnectionMock);

    when(linkMock.sendPacket(any())).thenReturn(InterledgerFulfillPacket.builder()
      .fulfillment(InterledgerFulfillment.of(new byte[32]))
      .build());
    InterledgerPreparePacket preparePacket = mock(InterledgerPreparePacket.class);
    StreamPacketReply result = streamPacketFilterChain.doFilter(StreamPacketRequest.builder()
      .interledgerPreparePacket(Optional.of(preparePacket))
      .sendState(SendState.Ready)
      .isFulfillable(true)
      .build());

    assertThat(result.exception()).isEmpty();
    assertThat(result.interledgerPreparePacket().get().getAmount()).isEqualTo(UnsignedLong.ZERO);
    assertThat(result.interledgerPreparePacket().get().getDestination())
      .isEqualTo(InterledgerAddress.of("private.test"));
    assertThat(((InterledgerFulfillPacket) result.interledgerResponsePacket().get()).getFulfillment())
      .isEqualTo(InterledgerFulfillment.of(new byte[32]));

    verify(paymentSharedStateTrackerMock, times(2)).getStreamConnection();
    verify(linkMock).sendPacket(any());
    verifyNoMoreInteractions(linkMock);
    verifyNoMoreInteractions(paymentSharedStateTrackerMock);
  }

  @Test
  public void doFilterWithSendStateReadyRejectionF08ThatCantDecode() {
    StreamConnection streamConnectionMock = mock(StreamConnection.class);
    when(streamConnectionMock.getStreamSharedSecret()).thenReturn(SharedSecret.of(new byte[32]));
    when(streamConnectionMock.getDestinationAddress()).thenReturn(InterledgerAddress.of("private.test"));
    when(paymentSharedStateTrackerMock.getStreamConnection()).thenReturn(streamConnectionMock);

    when(linkMock.sendPacket(any())).thenReturn(InterledgerRejectPacket.builder()
      .code(InterledgerErrorCode.F08_AMOUNT_TOO_LARGE)
      .message("foo")
      .build());
    InterledgerPreparePacket preparePacket = mock(InterledgerPreparePacket.class);
    StreamPacketReply result = streamPacketFilterChain.doFilter(StreamPacketRequest.builder()
      .interledgerPreparePacket(Optional.of(preparePacket))
      .sendState(SendState.Ready)
      .isFulfillable(false)
      .build());

    assertThat(result.exception()).isEmpty();
    assertThat(result.interledgerPreparePacket().get().getAmount()).isEqualTo(UnsignedLong.ZERO);
    assertThat(result.interledgerPreparePacket().get().getDestination())
      .isEqualTo(InterledgerAddress.of("private.test"));
    assertThat(((InterledgerRejectPacket) result.interledgerResponsePacket().get()).getCode())
      .isEqualTo(InterledgerErrorCode.F08_AMOUNT_TOO_LARGE);

    verify(paymentSharedStateTrackerMock).getStreamConnection();
    verify(linkMock).sendPacket(any());
    verifyNoMoreInteractions(linkMock);
    verifyNoMoreInteractions(paymentSharedStateTrackerMock);
  }

  @Test
  public void doFilterWithSendStateReadyRejectionF08Decodes() {
    StreamConnection streamConnectionMock = mock(StreamConnection.class);
    when(streamConnectionMock.getStreamSharedSecret()).thenReturn(SharedSecret.of(new byte[32]));
    when(streamConnectionMock.getDestinationAddress()).thenReturn(InterledgerAddress.of("private.test"));
    when(paymentSharedStateTrackerMock.getStreamConnection()).thenReturn(streamConnectionMock);

    when(linkMock.sendPacket(any())).thenReturn(InterledgerRejectPacket.builder()
      .code(InterledgerErrorCode.F08_AMOUNT_TOO_LARGE)
      .message("foo")
      .data(BaseEncoding.base64().decode("AAAAAAAAAAAAAAAAAAAAAQ=="))
      .build());
    InterledgerPreparePacket preparePacket = mock(InterledgerPreparePacket.class);

    final StreamPacketReply streamPacketReply = streamPacketFilterChain.doFilter(
      StreamPacketRequest.builder()
        .interledgerPreparePacket(Optional.of(preparePacket))
        .sendState(SendState.Ready)
        .isFulfillable(false)
        .build()
    );

    assertThat(streamPacketReply.exception()).isEmpty();
    assertThat(streamPacketReply.interledgerPreparePacket().get().getAmount()).isEqualTo(UnsignedLong.ZERO);
    assertThat(streamPacketReply.interledgerPreparePacket().get().getDestination())
      .isEqualTo(InterledgerAddress.of("private.test"));
    assertThat(((InterledgerRejectPacket) streamPacketReply.interledgerResponsePacket().get()).getCode())
      .isEqualTo(InterledgerErrorCode.F08_AMOUNT_TOO_LARGE);

    // The reply here should have an
    streamPacketReply.interledgerResponsePacket().get().handle(
      interledgerFulfillPacket -> fail("Should not go here"),
      interledgerRejectPacket -> {
        // Should be an F08 packet.
        AmountTooLargeErrorData amountTooLargeErrorData = ((AmountTooLargeErrorData) interledgerRejectPacket.typedData()
          .get());
        assertThat(amountTooLargeErrorData.receivedAmount()).isEqualTo(UnsignedLong.ZERO);
        assertThat(amountTooLargeErrorData.maximumAmount()).isEqualTo(UnsignedLong.ONE);
      }
    );

    verify(paymentSharedStateTrackerMock).getStreamConnection();
    verify(linkMock).sendPacket(any());
    verifyNoMoreInteractions(linkMock);
    verifyNoMoreInteractions(paymentSharedStateTrackerMock);
  }

  @Test
  public void doFilterWithSendStateReadyRejectionWithNoFrames() {
    StreamConnection streamConnectionMock = mock(StreamConnection.class);
    when(streamConnectionMock.getStreamSharedSecret()).thenReturn(SharedSecret.of(new byte[32]));
    when(streamConnectionMock.getDestinationAddress()).thenReturn(InterledgerAddress.of("private.test"));
    when(paymentSharedStateTrackerMock.getStreamConnection()).thenReturn(streamConnectionMock);

    when(linkMock.sendPacket(any())).thenReturn(InterledgerRejectPacket.builder()
      .code(InterledgerErrorCode.F00_BAD_REQUEST)
      .message("foo")
      //.data(BaseEncoding.base64().decode("AAAAAAAAAAAAAAAAAAAAAQ=="))
      .build());
    InterledgerPreparePacket preparePacket = mock(InterledgerPreparePacket.class);

    final StreamPacketReply streamPacketReply = streamPacketFilterChain.doFilter(
      StreamPacketRequest.builder()
        .interledgerPreparePacket(Optional.of(preparePacket))
        .sendState(SendState.Ready)
        .isFulfillable(false)
        .build()
    );

    assertThat(streamPacketReply.exception()).isEmpty();
    assertThat(streamPacketReply.interledgerPreparePacket().get().getAmount()).isEqualTo(UnsignedLong.ZERO);
    assertThat(streamPacketReply.interledgerPreparePacket().get().getDestination())
      .isEqualTo(InterledgerAddress.of("private.test"));
    assertThat(((InterledgerRejectPacket) streamPacketReply.interledgerResponsePacket().get()).getCode())
      .isEqualTo(InterledgerErrorCode.F00_BAD_REQUEST);

    // The reply here should have an
    streamPacketReply.interledgerResponsePacket().get().handle(
      interledgerFulfillPacket -> fail("Should not go here"),
      interledgerRejectPacket -> assertThat(interledgerRejectPacket.typedData()).isEmpty()
    );

    verify(paymentSharedStateTrackerMock, times(2)).getStreamConnection();
    verify(linkMock).sendPacket(any());
    verifyNoMoreInteractions(linkMock);
    verifyNoMoreInteractions(paymentSharedStateTrackerMock);
  }

  @Test
  public void doFilterWithSendStateReadyRejectionWith1Frame() {
    StreamConnection streamConnectionMock = mock(StreamConnection.class);
    when(streamConnectionMock.getStreamSharedSecret()).thenReturn(SharedSecret.of(new byte[32]));
    when(streamConnectionMock.getDestinationAddress()).thenReturn(InterledgerAddress.of("private.test"));
    when(paymentSharedStateTrackerMock.getStreamConnection()).thenReturn(streamConnectionMock);

    when(linkMock.sendPacket(any())).thenReturn(InterledgerRejectPacket.builder()
      .code(InterledgerErrorCode.F00_BAD_REQUEST)
      .message("foo")
      .data(BaseEncoding.base64().decode("nPzkxb3dbv1yuggbRNkyDivUj8Dpzi4m3IqgNCrbX/GhYV7z"))
      .build());
    InterledgerPreparePacket preparePacket = mock(InterledgerPreparePacket.class);

    final StreamPacketReply streamPacketReply = streamPacketFilterChain.doFilter(
      StreamPacketRequest.builder()
        .interledgerPreparePacket(Optional.of(preparePacket))
        .sendState(SendState.Ready)
        .isFulfillable(false)
        .build()
    );

    assertThat(streamPacketReply.exception()).isEmpty();
    assertThat(streamPacketReply.interledgerPreparePacket().get().getAmount()).isEqualTo(UnsignedLong.ZERO);
    assertThat(streamPacketReply.interledgerPreparePacket().get().getDestination())
      .isEqualTo(InterledgerAddress.of("private.test"));
    assertThat(((InterledgerRejectPacket) streamPacketReply.interledgerResponsePacket().get()).getCode())
      .isEqualTo(InterledgerErrorCode.F00_BAD_REQUEST);

    // The reply here should have an
    streamPacketReply.interledgerResponsePacket().get().handle(
      interledgerFulfillPacket -> fail("Should not go here"),
      interledgerRejectPacket -> assertThat(interledgerRejectPacket.typedData()).isPresent()
    );

    verify(paymentSharedStateTrackerMock, times(2)).getStreamConnection();
    verify(linkMock).sendPacket(any());
    verifyNoMoreInteractions(linkMock);
    verifyNoMoreInteractions(paymentSharedStateTrackerMock);
  }

  //////////////////
  // Private Helpers
  //////////////////

  private static class TestableStreamPacketFilter implements StreamPacketFilter {

    private final boolean throwExceptionInDoFilter;
    private final boolean throwExceptionInNextState;
    private final AtomicInteger numCalls = new AtomicInteger();

    private TestableStreamPacketFilter() {
      this(false, false);
    }

    private TestableStreamPacketFilter(boolean throwExceptionInNextState, boolean throwExceptionInDoFilter) {
      this.throwExceptionInNextState = throwExceptionInNextState;
      this.throwExceptionInDoFilter = throwExceptionInDoFilter;
    }

    @Override
    public SendState nextState(ModifiableStreamPacketRequest streamPacketRequest) {
      numCalls.getAndIncrement();
      if (throwExceptionInNextState) {
        throw new StreamPayerException(SendState.ConnectorError);
      } else {
        return SendState.Ready;
      }
    }

    @Override
    public StreamPacketReply doFilter(StreamPacketRequest streamPacketRequest, StreamPacketFilterChain filterChain) {
      numCalls.getAndIncrement();
      if (throwExceptionInDoFilter) {
        throw new StreamPayerException(SendState.InsufficientExchangeRate);
      } else {
        return filterChain.doFilter(streamPacketRequest);
      }
    }

    public AtomicInteger getNumCalls() {
      return numCalls;
    }
  }

}