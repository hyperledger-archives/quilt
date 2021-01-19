package org.interledger.stream.pay.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.stream.pay.filters.chain.StreamPacketFilterChain;
import org.interledger.stream.pay.model.ModifiableStreamPacketRequest;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.model.StreamPacketReply;
import org.interledger.stream.pay.model.StreamPacketRequest;
import org.interledger.stream.pay.trackers.PacingTracker;

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
 * Unit tests for {@link PacingFilter}.
 */
public class PacingFilterTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  PacingTracker pacingTrackerMock;

  private PacingFilter pacingFilter;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    this.pacingFilter = new PacingFilter(pacingTrackerMock);
  }

  @Test
  public void constructWithNull() {
    expectedException.expect(NullPointerException.class);
    new PacingFilter(null);
  }

  ////////////
  // nextState
  ////////////

  @Test
  public void nextStateWhenNull() {
    expectedException.expect(NullPointerException.class);
    this.pacingFilter.nextState(null);
  }


  @Test
  public void nextStateWhenNoMoreRequestsAvailable() {
    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    when(pacingTrackerMock.getNumberInFlight()).thenReturn(20);
    when(pacingTrackerMock.getNextPacketSendTime()).thenReturn(Instant.now().minus(30, ChronoUnit.SECONDS));

    assertThat(this.pacingFilter.nextState(request)).isEqualTo(SendState.Wait);
  }

  @Test
  public void nextStateWhenNextPacketSendTimeAfterNow() {
    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    when(pacingTrackerMock.getNumberInFlight()).thenReturn(0);
    when(pacingTrackerMock.getNextPacketSendTime()).thenReturn(Instant.now().plus(30, ChronoUnit.SECONDS));

    assertThat(this.pacingFilter.nextState(request)).isEqualTo(SendState.Wait);
  }

  @Test
  public void nextStateWhenMoreRequestsAvailable() {
    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    when(pacingTrackerMock.getNumberInFlight()).thenReturn(0);
    when(pacingTrackerMock.getNextPacketSendTime()).thenReturn(Instant.now().minus(30, ChronoUnit.SECONDS));

    assertThat(this.pacingFilter.nextState(request)).isEqualTo(SendState.Ready);
  }

  ////////////
  // doFilter
  ////////////

  @Test
  public void doFilterWhenNullStreamRequest() {
    expectedException.expect(NullPointerException.class);
    this.pacingFilter.doFilter(null, mock(StreamPacketFilterChain.class));
  }

  @Test
  public void doFilterWhenNullFilterChain() {
    expectedException.expect(NullPointerException.class);
    this.pacingFilter.doFilter(mock(StreamPacketRequest.class), null);
  }

  @Test
  public void doFilterIsNotAuthenticNoT04() {
    StreamPacketRequest streamPacketRequestMock = mock(StreamPacketRequest.class);
    StreamPacketReply streamPacketReply = mock(StreamPacketReply.class);
    when(streamPacketReply.isAuthentic()).thenReturn(false);

    StreamPacketFilterChain streamPacketFilterChainMock = mock(StreamPacketFilterChain.class);
    when(streamPacketFilterChainMock.doFilter(any())).thenReturn(streamPacketReply);

    StreamPacketReply actual = this.pacingFilter.doFilter(streamPacketRequestMock, streamPacketFilterChainMock);

    assertThat(actual).isEqualTo(streamPacketReply);

    verify(pacingTrackerMock).incrementNumPacketsInFlight();
    verify(pacingTrackerMock).decrementNumPacketsInFlight();
    verify(pacingTrackerMock).setLastPacketSentTime(any());

    verifyNoMoreInteractions(pacingTrackerMock);
  }

  @Test
  public void doFilterIsNotAuthenticWithT04() {
    StreamPacketRequest streamPacketRequestMock = mock(StreamPacketRequest.class);
    StreamPacketReply streamPacketReply = mock(StreamPacketReply.class);
    when(streamPacketReply.isAuthentic()).thenReturn(false);

    StreamPacketFilterChain streamPacketFilterChainMock = mock(StreamPacketFilterChain.class);
    when(streamPacketFilterChainMock.doFilter(any())).thenReturn(streamPacketReply);

    when(pacingTrackerMock.getPacketsPerSecond()).thenReturn(5);

    this.pacingFilter = new PacingFilter(pacingTrackerMock) {
      @Override
      boolean hasT04RejectCode(StreamPacketReply streamPacketReply) {
        return true;
      }
    };

    StreamPacketReply actual = this.pacingFilter.doFilter(streamPacketRequestMock, streamPacketFilterChainMock);

    assertThat(actual).isEqualTo(streamPacketReply);

    verify(pacingTrackerMock).incrementNumPacketsInFlight();
    verify(pacingTrackerMock).decrementNumPacketsInFlight();
    verify(pacingTrackerMock).setLastPacketSentTime(any());
    verify(pacingTrackerMock).getPacketsPerSecond();
    verify(pacingTrackerMock).setPacketsPerSecond(2);

    verifyNoMoreInteractions(pacingTrackerMock);
  }

  @Test
  public void doFilterIsAuthenticNoT04() {
    StreamPacketRequest streamPacketRequestMock = mock(StreamPacketRequest.class);
    StreamPacketReply streamPacketReply = mock(StreamPacketReply.class);
    when(streamPacketReply.isAuthentic()).thenReturn(true);

    StreamPacketFilterChain streamPacketFilterChainMock = mock(StreamPacketFilterChain.class);
    when(streamPacketFilterChainMock.doFilter(any())).thenReturn(streamPacketReply);

    when(pacingTrackerMock.getPacketsPerSecond()).thenReturn(1);

    StreamPacketReply actual = this.pacingFilter.doFilter(streamPacketRequestMock, streamPacketFilterChainMock);

    assertThat(actual).isEqualTo(streamPacketReply);

    verify(pacingTrackerMock).incrementNumPacketsInFlight();
    verify(pacingTrackerMock).decrementNumPacketsInFlight();
    verify(pacingTrackerMock).setLastPacketSentTime(any());
    verify(pacingTrackerMock).getPacketsPerSecond();
    verify(pacingTrackerMock).setPacketsPerSecond(2);
    verify(pacingTrackerMock).updateAverageRoundTripTime(anyInt());

    verifyNoMoreInteractions(pacingTrackerMock);
  }

  @Test
  public void doFilterIsAuthenticWithT04() {
    StreamPacketRequest streamPacketRequestMock = mock(StreamPacketRequest.class);
    StreamPacketReply streamPacketReply = mock(StreamPacketReply.class);
    when(streamPacketReply.isAuthentic()).thenReturn(true);

    StreamPacketFilterChain streamPacketFilterChainMock = mock(StreamPacketFilterChain.class);
    when(streamPacketFilterChainMock.doFilter(any())).thenReturn(streamPacketReply);

    when(pacingTrackerMock.getPacketsPerSecond()).thenReturn(5);

    this.pacingFilter = new PacingFilter(pacingTrackerMock) {
      @Override
      boolean hasT04RejectCode(StreamPacketReply streamPacketReply) {
        return true;
      }
    };

    StreamPacketReply actual = this.pacingFilter.doFilter(streamPacketRequestMock, streamPacketFilterChainMock);

    assertThat(actual).isEqualTo(streamPacketReply);

    verify(pacingTrackerMock).incrementNumPacketsInFlight();
    verify(pacingTrackerMock).decrementNumPacketsInFlight();
    verify(pacingTrackerMock).setLastPacketSentTime(any());
    verify(pacingTrackerMock).getPacketsPerSecond();
    verify(pacingTrackerMock).setPacketsPerSecond(2);
    verify(pacingTrackerMock).updateAverageRoundTripTime(anyInt());

    verifyNoMoreInteractions(pacingTrackerMock);
  }

  ///////////////////
  // hasT04RejectCode
  ///////////////////

  @Test
  public void hasT04RejectCodeWithNull() {
    expectedException.expect(NullPointerException.class);
    this.pacingFilter.hasT04RejectCode(null);
  }

  @Test
  public void hasT04RejectCodeWhenRejectWithT04() {
    StreamPacketReply streamPacketReply = mock(StreamPacketReply.class);
    InterledgerRejectPacket responsePacket = InterledgerRejectPacket.builder()
      .code(InterledgerErrorCode.T04_INSUFFICIENT_LIQUIDITY)
      .triggeredBy(InterledgerAddress.of("example.foo"))
      .build();
    when(streamPacketReply.interledgerResponsePacket()).thenReturn(Optional.of(responsePacket));
    assertThat(this.pacingFilter.hasT04RejectCode(streamPacketReply)).isTrue();
  }

  @Test
  public void hasT04RejectCodeWhenRejectWithT01() {
    StreamPacketReply streamPacketReply = mock(StreamPacketReply.class);
    InterledgerRejectPacket responsePacket = InterledgerRejectPacket.builder()
      .code(InterledgerErrorCode.T01_PEER_UNREACHABLE)
      .triggeredBy(InterledgerAddress.of("example.foo"))
      .build();
    when(streamPacketReply.interledgerResponsePacket()).thenReturn(Optional.of(responsePacket));
    assertThat(this.pacingFilter.hasT04RejectCode(streamPacketReply)).isFalse();
  }

  @Test
  public void hasT04RejectCodeWhenFulfill() {
    StreamPacketReply streamPacketReply = mock(StreamPacketReply.class);
    InterledgerFulfillPacket responsePacket = InterledgerFulfillPacket.builder()
      .fulfillment(InterledgerFulfillment.of(new byte[32]))
      .build();
    when(streamPacketReply.interledgerResponsePacket()).thenReturn(Optional.of(responsePacket));
    assertThat(this.pacingFilter.hasT04RejectCode(streamPacketReply)).isFalse();
  }
}