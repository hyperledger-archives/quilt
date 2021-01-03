package org.interledger.stream.pay.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.interledger.stream.pay.filters.chain.StreamPacketFilterChain;
import org.interledger.stream.pay.model.ModifiableStreamPacketRequest;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.model.StreamPacketReply;
import org.interledger.stream.pay.model.StreamPacketRequest;

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
  public void doFilterIsNotAuthenticNoT04() {
    StreamPacketRequest streamPacketRequestMock = mock(StreamPacketRequest.class);
    StreamPacketReply streamPacketReply = mock(StreamPacketReply.class);
    when(streamPacketReply.isAuthentic()).thenReturn(false);

    StreamPacketFilterChain streamPacketFilterChainMock = mock(StreamPacketFilterChain.class);
    when(streamPacketFilterChainMock.doFilter(any())).thenReturn(streamPacketReply);

    StreamPacketReply actual = this.failureFilter.doFilter(streamPacketRequestMock, streamPacketFilterChainMock);

    assertThat(actual).isEqualTo(streamPacketReply);
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
//    assertThat(failureFilter.isRemoteClosed()).isFalse();
    // TODO
  }

  @Test
  public void handleRemoteCloseWithRequiredFrame() {
    this.failureFilter.handleRemoteClose(Lists.newArrayList());
//    assertThat(failureFilter.isRemoteClosed()).isFalse();
    // TODO
  }

  @Test
  public void handleRemoteCloseWithoutRequiredFrame() {
    this.failureFilter.handleRemoteClose(Lists.newArrayList());
//    assertThat(failureFilter.isRemoteClosed()).isFalse();
    // TODO
  }

}