package org.interledger.stream.pay.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.stream.pay.StreamPayerExceptionMatcher.hasSendState;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.stream.frames.ConnectionCloseFrame;
import org.interledger.stream.frames.ErrorCodes;
import org.interledger.stream.frames.StreamMoneyMaxFrame;
import org.interledger.stream.pay.exceptions.StreamPayerException;
import org.interledger.stream.pay.filters.chain.StreamPacketFilterChain;
import org.interledger.stream.pay.model.ModifiableStreamPacketRequest;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.model.StreamPacketReply;
import org.interledger.stream.pay.model.StreamPacketRequest;
import org.interledger.stream.pay.trackers.StatisticsTracker;

import com.google.common.primitives.UnsignedLong;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Unit tests for {@link FailureFilter}.
 */
public class FailureFilterTest {

  @Mock
  private StatisticsTracker statisticsTrackerMock;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private FailureFilter failureFilter;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    when(statisticsTrackerMock.getNumFulfills()).thenReturn(0);
    when(statisticsTrackerMock.getNumRejects()).thenReturn(0);

    this.failureFilter = new FailureFilter(statisticsTrackerMock);
  }

  ////////////
  // nextState
  ////////////

  @Test
  public void nextStateWhenNull() {
    expectedException.expect(NullPointerException.class);
    this.failureFilter.nextState(null);
  }

  @Test
  public void nextStateWhenTerminalReject() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.ConnectorError));
    expectedException.expectMessage("Terminal rejection encountered.");

    this.failureFilter = new FailureFilter(statisticsTrackerMock) {
      @Override
      boolean terminalRejectEncountered() {
        return true;
      }
    };
    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    assertThat(this.failureFilter.nextState(request)).isEqualTo(SendState.ConnectorError);
  }

  @Test
  public void nextStateRemoteClosed() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.ClosedByRecipient));
    expectedException.expectMessage("Remote connection was closed by the receiver.");

    this.failureFilter = new FailureFilter(statisticsTrackerMock) {
      @Override
      boolean remoteClosed() {
        return true;
      }
    };
    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    assertThat(this.failureFilter.nextState(request)).isEqualTo(SendState.ClosedByRecipient);
  }

  @Test
  public void nextStateWhenSomeRejectionsButNotTooMany() {
    when(statisticsTrackerMock.getNumRejects()).thenReturn(4);
    when(statisticsTrackerMock.getNumFulfills()).thenReturn(100);
    when(statisticsTrackerMock.getTotalPacketResponses()).thenReturn(104);

    this.failureFilter = new FailureFilter(statisticsTrackerMock);

    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    assertThat(this.failureFilter.nextState(request)).isEqualTo(SendState.Ready);
  }

  /**
   * See implementation note in {@link FailureFilter}. This implementation doesn't end when the % of failures is too
   * high. Instead, it relies on the time since last fulfill, and terminal rejects.
   */
  @Test
  public void nextStateWhenTooManyRejections() {
    when(statisticsTrackerMock.getNumRejects()).thenReturn(40);
    when(statisticsTrackerMock.getNumFulfills()).thenReturn(60);
    when(statisticsTrackerMock.getTotalPacketResponses()).thenReturn(100);

    this.failureFilter = new FailureFilter(statisticsTrackerMock);

    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    assertThat(this.failureFilter.nextState(request)).isEqualTo(SendState.Ready);
  }

  @Test
  public void nextStateWhenEmptyLastFulfillmentTime() {
    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    assertThat(this.failureFilter.nextState(request)).isEqualTo(SendState.Ready);
  }

  @Test
  public void nextStateWhenLastFulfillmentTimeAfterDeadline() {
    when(statisticsTrackerMock.getTotalPacketResponses()).thenReturn(0);

    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.IdleTimeout));
    expectedException.expectMessage("Ending payment because no Fulfill was received before idle deadline.");

    this.failureFilter = new FailureFilter(statisticsTrackerMock) {
      @Override
      protected Optional<Instant> getLastFulfillmentTime() {
        return Optional.of(Instant.now().minus(30, ChronoUnit.MINUTES));
      }
    };

    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    assertThat(this.failureFilter.nextState(request)).isEqualTo(SendState.IdleTimeout);
  }

  @Test
  public void nextStateWhenLastFulfillmentTimeBeforeDeadline() {
    this.failureFilter = new FailureFilter(statisticsTrackerMock) {
      @Override
      protected Optional<Instant> getLastFulfillmentTime() {
        return Optional.of(Instant.now());
      }
    };

    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    assertThat(this.failureFilter.nextState(request)).isEqualTo(SendState.Ready);
  }

  ////////////
  // doFilter
  ////////////

  @Test
  public void doFilterWhenNullStreamRequest() {
    expectedException.expect(NullPointerException.class);
    this.failureFilter.doFilter(null, mock(StreamPacketFilterChain.class));
  }

  @Test
  public void doFilterWhenNullFilterChain() {
    expectedException.expect(NullPointerException.class);
    this.failureFilter.doFilter(mock(StreamPacketRequest.class), null);
  }

  @Test
  public void doFilterWithFulfill() {
    StreamPacketRequest streamPacketRequestMock = mock(StreamPacketRequest.class);
    StreamPacketReply streamPacketReply = mock(StreamPacketReply.class);
    when(streamPacketReply.isAuthentic()).thenReturn(false);
    when(streamPacketReply.isReject()).thenReturn(true);
    InterledgerResponsePacket interledgerResponsePacket = InterledgerFulfillPacket.builder()
      .fulfillment(InterledgerFulfillment.of(new byte[32]))
      .build();
    when(streamPacketReply.interledgerResponsePacket()).thenReturn(Optional.of(interledgerResponsePacket));

    StreamPacketFilterChain streamPacketFilterChainMock = mock(StreamPacketFilterChain.class);
    when(streamPacketFilterChainMock.doFilter(any())).thenReturn(streamPacketReply);

    StreamPacketReply actual = this.failureFilter.doFilter(streamPacketRequestMock, streamPacketFilterChainMock);
    assertThat(actual).isEqualTo(streamPacketReply);

    assertThat(failureFilter.getLastFulfillmentTime()).isPresent();
    assertThat(failureFilter.remoteClosed()).isFalse();
    assertThat(failureFilter.terminalRejectEncountered()).isFalse();

    verify(statisticsTrackerMock).incrementNumFulfills();
    verifyNoMoreInteractions(statisticsTrackerMock);
  }

  @Test
  public void doFilterWithRejectT00() {
    StreamPacketRequest streamPacketRequestMock = mock(StreamPacketRequest.class);
    StreamPacketReply streamPacketReply = mock(StreamPacketReply.class);
    when(streamPacketReply.isAuthentic()).thenReturn(false);
    when(streamPacketReply.isReject()).thenReturn(true);
    InterledgerResponsePacket interledgerResponsePacket = InterledgerRejectPacket.builder()
      .triggeredBy(InterledgerAddress.of("example.foo"))
      .code(InterledgerErrorCode.T00_INTERNAL_ERROR)
      .build();
    when(streamPacketReply.interledgerResponsePacket()).thenReturn(Optional.of(interledgerResponsePacket));

    StreamPacketFilterChain streamPacketFilterChainMock = mock(StreamPacketFilterChain.class);
    when(streamPacketFilterChainMock.doFilter(any())).thenReturn(streamPacketReply);

    StreamPacketReply actual = this.failureFilter.doFilter(streamPacketRequestMock, streamPacketFilterChainMock);
    assertThat(actual).isEqualTo(streamPacketReply);

    assertThat(failureFilter.getLastFulfillmentTime()).isPresent();
    assertThat(failureFilter.remoteClosed()).isFalse();
    assertThat(failureFilter.terminalRejectEncountered()).isFalse();

    verify(statisticsTrackerMock).incrementNumRejects();
    verifyNoMoreInteractions(statisticsTrackerMock);
  }

  @Test
  public void doFilterWithRejectF08() {
    StreamPacketRequest streamPacketRequestMock = mock(StreamPacketRequest.class);
    StreamPacketReply streamPacketReply = mock(StreamPacketReply.class);
    when(streamPacketReply.isAuthentic()).thenReturn(false);
    when(streamPacketReply.isReject()).thenReturn(true);
    InterledgerResponsePacket interledgerResponsePacket = InterledgerRejectPacket.builder()
      .triggeredBy(InterledgerAddress.of("example.foo"))
      .code(InterledgerErrorCode.F08_AMOUNT_TOO_LARGE)
      .build();
    when(streamPacketReply.interledgerResponsePacket()).thenReturn(Optional.of(interledgerResponsePacket));

    StreamPacketFilterChain streamPacketFilterChainMock = mock(StreamPacketFilterChain.class);
    when(streamPacketFilterChainMock.doFilter(any())).thenReturn(streamPacketReply);

    StreamPacketReply actual = this.failureFilter.doFilter(streamPacketRequestMock, streamPacketFilterChainMock);
    assertThat(actual).isEqualTo(streamPacketReply);

    assertThat(failureFilter.getLastFulfillmentTime()).isPresent();
    assertThat(failureFilter.remoteClosed()).isFalse();
    assertThat(failureFilter.terminalRejectEncountered()).isFalse();
    verify(statisticsTrackerMock).incrementNumRejects();
    verifyNoMoreInteractions(statisticsTrackerMock);
  }

  @Test
  public void doFilterWithRejectF99() {
    StreamPacketRequest streamPacketRequestMock = mock(StreamPacketRequest.class);
    StreamPacketReply streamPacketReply = mock(StreamPacketReply.class);
    when(streamPacketReply.isAuthentic()).thenReturn(false);
    when(streamPacketReply.isReject()).thenReturn(true);
    InterledgerResponsePacket interledgerResponsePacket = InterledgerRejectPacket.builder()
      .triggeredBy(InterledgerAddress.of("example.foo"))
      .code(InterledgerErrorCode.F99_APPLICATION_ERROR)
      .build();
    when(streamPacketReply.interledgerResponsePacket()).thenReturn(Optional.of(interledgerResponsePacket));

    StreamPacketFilterChain streamPacketFilterChainMock = mock(StreamPacketFilterChain.class);
    when(streamPacketFilterChainMock.doFilter(any())).thenReturn(streamPacketReply);

    StreamPacketReply actual = this.failureFilter.doFilter(streamPacketRequestMock, streamPacketFilterChainMock);
    assertThat(actual).isEqualTo(streamPacketReply);

    assertThat(failureFilter.getLastFulfillmentTime()).isPresent();
    assertThat(failureFilter.remoteClosed()).isFalse();
    assertThat(failureFilter.terminalRejectEncountered()).isFalse();
    verify(statisticsTrackerMock).incrementNumRejects();
    verifyNoMoreInteractions(statisticsTrackerMock);
  }

  @Test
  public void doFilterWithRejectF04() {
    StreamPacketRequest streamPacketRequestMock = mock(StreamPacketRequest.class);
    StreamPacketReply streamPacketReply = mock(StreamPacketReply.class);
    when(streamPacketReply.isAuthentic()).thenReturn(false);
    when(streamPacketReply.isReject()).thenReturn(true);
    InterledgerResponsePacket interledgerResponsePacket = InterledgerRejectPacket.builder()
      .triggeredBy(InterledgerAddress.of("example.foo"))
      .code(InterledgerErrorCode.F04_INSUFFICIENT_DST_AMOUNT)
      .build();
    when(streamPacketReply.interledgerResponsePacket()).thenReturn(Optional.of(interledgerResponsePacket));

    StreamPacketFilterChain streamPacketFilterChainMock = mock(StreamPacketFilterChain.class);
    when(streamPacketFilterChainMock.doFilter(any())).thenReturn(streamPacketReply);

    StreamPacketReply actual = this.failureFilter.doFilter(streamPacketRequestMock, streamPacketFilterChainMock);
    assertThat(actual).isEqualTo(streamPacketReply);

    assertThat(failureFilter.getLastFulfillmentTime()).isPresent();
    assertThat(failureFilter.remoteClosed()).isFalse();
    assertThat(failureFilter.terminalRejectEncountered()).isTrue();
    verify(statisticsTrackerMock).incrementNumRejects();
    verifyNoMoreInteractions(statisticsTrackerMock);
  }

  ///////////////////
  // handleRemoteClose
  ///////////////////

  @Test
  public void handleRemoteCloseWithNullArg() {
    expectedException.expect(NullPointerException.class);
    this.failureFilter.handleRemoteClose(null);
  }

  @Test
  public void handleRemoteCloseWithEmptyFrames() {
    this.failureFilter.handleRemoteClose(Lists.newArrayList());
    assertThat(failureFilter.remoteClosed()).isFalse();
  }

  @Test
  public void handleRemoteCloseWithRequiredFrame() {
    this.failureFilter.handleRemoteClose(Lists.newArrayList(
      ConnectionCloseFrame.builder().errorCode(ErrorCodes.NoError).build()
    ));
    assertThat(failureFilter.remoteClosed()).isTrue();
  }

  @Test
  public void handleRemoteCloseWithoutRequiredFrame() {
    this.failureFilter.handleRemoteClose(Lists.newArrayList(StreamMoneyMaxFrame.builder()
      .streamId(UnsignedLong.ONE)
      .receiveMax(UnsignedLong.ONE)
      .totalReceived(UnsignedLong.ONE)
      .build()
    ));
    assertThat(failureFilter.remoteClosed()).isFalse();
  }
}