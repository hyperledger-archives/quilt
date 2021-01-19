package org.interledger.stream.pay.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.interledger.stream.pay.filters.chain.StreamPacketFilterChain;
import org.interledger.stream.pay.model.ModifiableStreamPacketRequest;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.model.StreamPacketReply;
import org.interledger.stream.pay.model.StreamPacketRequest;
import org.interledger.stream.pay.trackers.ExchangeRateTracker;

import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

/**
 * Unit tests for {@link ExchangeRateFilter}.
 */
public class ExchangeRateFilterTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  ExchangeRateTracker exchangeRateTrackerMock;

  private ExchangeRateFilter exchangeRateFilter;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    this.exchangeRateFilter = new ExchangeRateFilter(exchangeRateTrackerMock);
  }

  @Test
  public void constructWithNull() {
    expectedException.expect(NullPointerException.class);
    new ExchangeRateFilter(null);
  }

  ////////////
  // nextState
  ////////////

  @Test
  public void nextStateWhenNull() {
    expectedException.expect(NullPointerException.class);
    exchangeRateFilter.nextState(null);
  }

  @Test
  public void nextState() {
    assertThat(exchangeRateFilter.nextState(ModifiableStreamPacketRequest.create())).isEqualTo(SendState.Ready);
  }

  ////////////
  // doFilter
  ////////////

  @Test
  public void doFilterWhenNullStreamRequest() {
    expectedException.expect(NullPointerException.class);
    this.exchangeRateFilter.doFilter(null, mock(StreamPacketFilterChain.class));
  }

  @Test
  public void doFilterWhenNullFilterChain() {
    expectedException.expect(NullPointerException.class);
    this.exchangeRateFilter.doFilter(mock(StreamPacketRequest.class), null);
  }

  @Test
  public void doFilterWhenSourceAmountIsZero() {
    StreamPacketRequest streamPacketRequestMock = mock(StreamPacketRequest.class);
    when(streamPacketRequestMock.sourceAmount()).thenReturn(UnsignedLong.ZERO);

    StreamPacketReply streamPacketReply = mock(StreamPacketReply.class);

    StreamPacketFilterChain filterChainMock = mock(StreamPacketFilterChain.class);
    when(filterChainMock.doFilter(any())).thenReturn(streamPacketReply);

    StreamPacketReply actual = exchangeRateFilter.doFilter(streamPacketRequestMock, filterChainMock);
    assertThat(actual).isEqualTo(streamPacketReply);

    verifyZeroInteractions(exchangeRateTrackerMock);
  }

  @Test
  public void doFilterWithAbsentClaimedAmount() {
    StreamPacketRequest streamPacketRequestMock = mock(StreamPacketRequest.class);
    when(streamPacketRequestMock.sourceAmount()).thenReturn(UnsignedLong.ONE);

    StreamPacketReply streamPacketReply = mock(StreamPacketReply.class);
    when(streamPacketReply.isAuthentic()).thenReturn(true);
    when(streamPacketReply.destinationAmountClaimed()).thenReturn(Optional.empty());

    StreamPacketFilterChain filterChainMock = mock(StreamPacketFilterChain.class);
    when(filterChainMock.doFilter(any())).thenReturn(streamPacketReply);

    StreamPacketReply actual = exchangeRateFilter.doFilter(streamPacketRequestMock, filterChainMock);
    assertThat(actual).isEqualTo(streamPacketReply);

    verifyZeroInteractions(exchangeRateTrackerMock);
  }

  @Test
  public void doFilterWithClaimedAmount() {
    StreamPacketRequest streamPacketRequestMock = mock(StreamPacketRequest.class);
    when(streamPacketRequestMock.sourceAmount()).thenReturn(UnsignedLong.ONE);

    StreamPacketReply streamPacketReply = mock(StreamPacketReply.class);
    when(streamPacketReply.isAuthentic()).thenReturn(true);
    when(streamPacketReply.destinationAmountClaimed()).thenReturn(Optional.of(UnsignedLong.MAX_VALUE));

    StreamPacketFilterChain filterChainMock = mock(StreamPacketFilterChain.class);
    when(filterChainMock.doFilter(any())).thenReturn(streamPacketReply);

    StreamPacketReply actual = exchangeRateFilter.doFilter(streamPacketRequestMock, filterChainMock);
    assertThat(actual).isEqualTo(streamPacketReply);

    verify(exchangeRateTrackerMock).updateRate(UnsignedLong.ONE, UnsignedLong.MAX_VALUE);
    verifyNoMoreInteractions(exchangeRateTrackerMock);
  }

  @Test
  public void doFilterWithNonAuthenticStreamReply() {
    StreamPacketRequest streamPacketRequestMock = mock(StreamPacketRequest.class);
    when(streamPacketRequestMock.sourceAmount()).thenReturn(UnsignedLong.ONE);

    StreamPacketReply streamPacketReply = mock(StreamPacketReply.class);
    when(streamPacketReply.isAuthentic()).thenReturn(false);
    when(streamPacketReply.destinationAmountClaimed()).thenReturn(Optional.of(UnsignedLong.MAX_VALUE));

    StreamPacketFilterChain filterChainMock = mock(StreamPacketFilterChain.class);
    when(filterChainMock.doFilter(any())).thenReturn(streamPacketReply);

    StreamPacketReply actual = exchangeRateFilter.doFilter(streamPacketRequestMock, filterChainMock);
    assertThat(actual).isEqualTo(streamPacketReply);

    verifyNoMoreInteractions(exchangeRateTrackerMock);
  }
}