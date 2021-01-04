package org.interledger.stream.pay.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import org.interledger.stream.pay.filters.chain.StreamPacketFilterChain;
import org.interledger.stream.pay.model.ModifiableStreamPacketRequest;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.model.StreamPacketReply;
import org.interledger.stream.pay.model.StreamPacketRequest;

import com.google.common.primitives.UnsignedLong;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Unit tests for {@link FailureFilter}.
 */
public class FailureFilterTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private FailureFilter failureFilter;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    this.failureFilter = new FailureFilter();
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
    this.failureFilter = new FailureFilter() {
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
    this.failureFilter = new FailureFilter() {
      @Override
      boolean remoteClosed() {
        return true;
      }
    };
    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    assertThat(this.failureFilter.nextState(request)).isEqualTo(SendState.ClosedByRecipient);
  }

  @Test
  public void nextStateWhenTooManyRejections() {
    this.failureFilter = new FailureFilter() {
      @Override
      int getNumFulfills() {
        return 4;
      }

      @Override
      int getNumRejects() {
        return 100;
      }
    };
    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    assertThat(this.failureFilter.nextState(request)).isEqualTo(SendState.End);
  }

  @Test
  public void nextStateWhenEmptyLastFulfilllmentTime() {
    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    assertThat(this.failureFilter.nextState(request)).isEqualTo(SendState.Ready);
  }

  @Test
  public void nextStateWhenLastFulfilllmentTimeAfterDeadline() {
    this.failureFilter = new FailureFilter() {
      @Override
      Optional<Instant> getLastFulfillmentTime() {
        return Optional.of(Instant.now().minus(30, ChronoUnit.MINUTES));
      }
    };

    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    assertThat(this.failureFilter.nextState(request)).isEqualTo(SendState.IdleTimeout);
  }

  @Test
  public void nextStateWhenLastFulfilllmentTimeBeforeDeadline() {
    this.failureFilter = new FailureFilter() {
      @Override
      Optional<Instant> getLastFulfillmentTime() {
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
    assertThat(failureFilter.getNumFulfills()).isEqualTo(1);
    assertThat(failureFilter.getNumRejects()).isEqualTo(0);
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
    assertThat(failureFilter.getNumFulfills()).isEqualTo(0);
    assertThat(failureFilter.getNumRejects()).isEqualTo(1);
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
    assertThat(failureFilter.getNumFulfills()).isEqualTo(0);
    assertThat(failureFilter.getNumRejects()).isEqualTo(1);
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
    assertThat(failureFilter.getNumFulfills()).isEqualTo(0);
    assertThat(failureFilter.getNumRejects()).isEqualTo(1);
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
    assertThat(failureFilter.getNumFulfills()).isEqualTo(0);
    assertThat(failureFilter.getNumRejects()).isEqualTo(1);
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