package org.interledger.stream.pay.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.stream.pay.StreamPayerExceptionMatcher.hasErrorCode;
import static org.interledger.stream.pay.StreamPayerExceptionMatcher.hasSendState;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.interledger.stream.connection.StreamConnection;
import org.interledger.stream.frames.ErrorCodes;
import org.interledger.stream.pay.exceptions.StreamPayerException;
import org.interledger.stream.pay.filters.chain.StreamPacketFilterChain;
import org.interledger.stream.pay.model.ModifiableStreamPacketRequest;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.model.StreamPacketRequest;
import org.interledger.stream.pay.trackers.PaymentSharedStateTracker;

import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link SequenceFilter}.
 */
public class SequenceFilterTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private StreamConnection streamConnectionMock;

  @Mock
  private PaymentSharedStateTracker paymentSharedStateTrackerMock;

  private SequenceFilter sequenceFilter;

  /**
   * Setup method.
   */
  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);

    when(paymentSharedStateTrackerMock.getStreamConnection()).thenReturn(streamConnectionMock);

    sequenceFilter = new SequenceFilter(paymentSharedStateTrackerMock);
  }

  ////////////
  // nextState
  ////////////

  @Test
  public void nextStateWhenNull() {
    expectedException.expect(NullPointerException.class);
    this.sequenceFilter.nextState(null);
  }

  @Test
  public void nextStateUnderPacketLimit() {
    when(streamConnectionMock.nextSequence()).thenReturn(UnsignedLong.ONE);

    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    SendState response = sequenceFilter.nextState(request);

    assertThat(request.sequence()).isEqualTo(UnsignedLong.ONE);
    assertThat(response).isEqualTo(SendState.Ready);
  }

  @Test
  public void nextStateOverPacketLimit() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.ExceededMaxSequence));
    expectedException.expect(hasErrorCode(ErrorCodes.ProtocolViolation));
    expectedException.expectMessage("Ending payment (cannot exceed max safe sequence number).");

    when(streamConnectionMock.nextSequence()).thenReturn(UnsignedLong.MAX_VALUE);

    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    sequenceFilter.nextState(request);
  }

  ////////////
  // doFilter
  ////////////

  @Test
  public void doFilterWhenNullStreamRequest() {
    expectedException.expect(NullPointerException.class);
    this.sequenceFilter.doFilter(null, mock(StreamPacketFilterChain.class));
  }

  @Test
  public void doFilterWhenNullFilterChain() {
    expectedException.expect(NullPointerException.class);
    this.sequenceFilter.doFilter(mock(StreamPacketRequest.class), null);
  }

  @Test
  public void doFilter() {
    StreamPacketFilterChain filterChainMock = mock(StreamPacketFilterChain.class);
    this.sequenceFilter.doFilter(mock(StreamPacketRequest.class), filterChainMock);

    verify(filterChainMock).doFilter(any());
    verifyNoMoreInteractions(filterChainMock);
  }
}