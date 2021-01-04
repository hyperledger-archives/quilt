package org.interledger.stream.pay.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPacketType;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.stream.StreamPacket;
import org.interledger.stream.frames.ErrorCodes;
import org.interledger.stream.pay.filters.chain.StreamPacketFilterChain;
import org.interledger.stream.pay.model.ModifiableStreamPacketRequest;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.model.StreamPacketReply;
import org.interledger.stream.pay.model.StreamPacketRequest;
import org.interledger.stream.pay.trackers.MaxPacketAmountTracker;

import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link MaxPacketAmountFilter}.
 */
public class MaxPacketAmountFilterTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  MaxPacketAmountTracker maxPacketAmountTrackerMock;

  private MaxPacketAmountFilter maxPacketAmountFilter;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    this.maxPacketAmountFilter = new MaxPacketAmountFilter(maxPacketAmountTrackerMock);
  }

  @Test
  public void constructWithNull() {
    expectedException.expect(NullPointerException.class);
    new MaxPacketAmountFilter(null);
  }

  ////////////
  // nextState
  ////////////

  @Test
  public void nextStateWhenNull() {
    expectedException.expect(NullPointerException.class);
    this.maxPacketAmountFilter.nextState(null);
  }

  @Test
  public void nextStateWhenRemoteAssetDetailsChanged() {
    when(maxPacketAmountTrackerMock.getNoCapacityAvailable()).thenReturn(true);

    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();

    SendState response = this.maxPacketAmountFilter.nextState(request);
    assertThat(response).isEqualTo(SendState.ConnectorError);
    assertThat(request.streamErrorCodeForConnectionClose()).isEqualTo(ErrorCodes.NoError);
  }

  @Test
  public void nextStateWhenRemoteAssetDetailsNotChanged() {
    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    SendState response = this.maxPacketAmountFilter.nextState(request);
    assertThat(response).isEqualTo(SendState.Ready);
  }

  ////////////
  // doFilter
  ////////////

  @Test
  public void doFilterWhenNullStreamRequest() {
    expectedException.expect(NullPointerException.class);
    this.maxPacketAmountFilter.doFilter(null, mock(StreamPacketFilterChain.class));
  }

  @Test
  public void doFilterWhenNullFilterChain() {
    expectedException.expect(NullPointerException.class);
    this.maxPacketAmountFilter.doFilter(mock(StreamPacketRequest.class), null);
  }

  @Test
  public void doFilterWithFulfill() {
    StreamPacketRequest streamPacketRequestMock = mock(StreamPacketRequest.class);
    StreamPacketReply streamPacketReplyMock = StreamPacketReply.builder()
      .interledgerResponsePacket(
        InterledgerFulfillPacket.builder()
          .fulfillment(InterledgerFulfillment.of(new byte[32]))
          .build()
      )
      .build();

    StreamPacketFilterChain filterChainMock = mock(StreamPacketFilterChain.class);
    when(filterChainMock.doFilter(any())).thenReturn(streamPacketReplyMock);

    StreamPacketReply actual = this.maxPacketAmountFilter.doFilter(streamPacketRequestMock, filterChainMock);

    assertThat(actual).isEqualTo(streamPacketReplyMock);
    verify(maxPacketAmountTrackerMock).adjustPathCapacity(any());
    verifyNoMoreInteractions(maxPacketAmountTrackerMock);
  }

  @Test
  public void doFilterWithRejectWithF08() {
    StreamPacketRequest streamPacketRequestMock = mock(StreamPacketRequest.class);
    StreamPacketReply streamPacketReplyMock = StreamPacketReply.builder()
      .interledgerResponsePacket(
        InterledgerRejectPacket.builder()
          .code(InterledgerErrorCode.F08_AMOUNT_TOO_LARGE)
          .build()
      )
      .build();

    StreamPacketFilterChain filterChainMock = mock(StreamPacketFilterChain.class);
    when(filterChainMock.doFilter(any())).thenReturn(streamPacketReplyMock);

    StreamPacketReply actual = this.maxPacketAmountFilter.doFilter(streamPacketRequestMock, filterChainMock);

    assertThat(actual).isEqualTo(streamPacketReplyMock);
    verify(maxPacketAmountTrackerMock).reduceMaxPacketAmount(any(), any());
    verifyNoMoreInteractions(maxPacketAmountTrackerMock);
  }

  @Test
  public void doFilterWithRejectWithNonF08AndNotAuthentic() {
    StreamPacketRequest streamPacketRequestMock = mock(StreamPacketRequest.class);
    StreamPacketReply streamPacketReplyMock = StreamPacketReply.builder()
      .interledgerResponsePacket(
        InterledgerRejectPacket.builder()
          .code(InterledgerErrorCode.F00_BAD_REQUEST)
          .build()
      )
      .build();

    StreamPacketFilterChain filterChainMock = mock(StreamPacketFilterChain.class);
    when(filterChainMock.doFilter(any())).thenReturn(streamPacketReplyMock);

    StreamPacketReply actual = this.maxPacketAmountFilter.doFilter(streamPacketRequestMock, filterChainMock);

    assertThat(actual).isEqualTo(streamPacketReplyMock);
    verifyNoMoreInteractions(maxPacketAmountTrackerMock);
  }

  @Test
  public void doFilterWithRejectWithNonF08AndAuthentic() {
    StreamPacketRequest streamPacketRequestMock = mock(StreamPacketRequest.class);
    StreamPacketReply streamPacketReplyMock = StreamPacketReply.builder()
      .interledgerResponsePacket(
        InterledgerRejectPacket.builder()
          .code(InterledgerErrorCode.F00_BAD_REQUEST)
          .typedData(StreamPacket.builder()
            .sequence(UnsignedLong.ONE)
            .prepareAmount(UnsignedLong.ONE)
            .interledgerPacketType(InterledgerPacketType.REJECT)
            .build())
          .build()
      )
      .build();

    StreamPacketFilterChain filterChainMock = mock(StreamPacketFilterChain.class);
    when(filterChainMock.doFilter(any())).thenReturn(streamPacketReplyMock);

    StreamPacketReply actual = this.maxPacketAmountFilter.doFilter(streamPacketRequestMock, filterChainMock);

    assertThat(actual).isEqualTo(streamPacketReplyMock);
    verify(maxPacketAmountTrackerMock).adjustPathCapacity(any());
    verifyNoMoreInteractions(maxPacketAmountTrackerMock);
  }
}