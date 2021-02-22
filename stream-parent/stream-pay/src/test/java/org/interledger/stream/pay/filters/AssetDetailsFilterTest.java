package org.interledger.stream.pay.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.stream.pay.StreamPayerExceptionMatcher.hasSendState;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.interledger.core.InterledgerAddress;
import org.interledger.fx.Denominations;
import org.interledger.stream.frames.ConnectionNewAddressFrame;
import org.interledger.stream.frames.StreamFrame;
import org.interledger.stream.frames.StreamFrameType;
import org.interledger.stream.model.AccountDetails;
import org.interledger.stream.pay.exceptions.StreamPayerException;
import org.interledger.stream.pay.filters.chain.StreamPacketFilterChain;
import org.interledger.stream.pay.model.ModifiableStreamPacketRequest;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.model.StreamPacketReply;
import org.interledger.stream.pay.model.StreamPacketRequest;
import org.interledger.stream.pay.trackers.AssetDetailsTracker;
import org.interledger.stream.pay.trackers.PaymentSharedStateTracker;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link AssetDetailsFilter}.
 */
public class AssetDetailsFilterTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  PaymentSharedStateTracker paymentSharedStateTrackerMock;

  @Mock
  AssetDetailsTracker assetDetailsTrackerMock;

  private final InterledgerAddress SENDER_ADDRESS = InterledgerAddress.of("example.sender");

  private final InterledgerAddress RECEIVER_ADDRESS = InterledgerAddress.of("example.receiver");

  private AssetDetailsFilter assetDetailsFilter;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    when(paymentSharedStateTrackerMock.getAssetDetailsTracker()).thenReturn(assetDetailsTrackerMock);

    when(assetDetailsTrackerMock.getDestinationAccountDetails()).thenReturn(AccountDetails.builder()
      .denomination(Denominations.XRP_DROPS)
      .interledgerAddress(RECEIVER_ADDRESS)
      .build());

    when(assetDetailsTrackerMock.getSourceAccountDetails()).thenReturn(AccountDetails.builder()
      .denomination(Denominations.USD_CENTS)
      .interledgerAddress(SENDER_ADDRESS)
      .build());

    this.assetDetailsFilter = new AssetDetailsFilter(paymentSharedStateTrackerMock);
  }

  ////////////
  // nextState
  ////////////

  @Test
  public void nextStateWhenNull() {
    expectedException.expect(NullPointerException.class);
    this.assetDetailsFilter.nextState(null);
  }

  @Test
  public void nextStateWhenRemoteAssetDetailsChanged() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.DestinationAssetConflict));
    expectedException.expectMessage("Destination asset changed, but this is prohibited by the IL-RFC-29.");

    this.assetDetailsFilter = new AssetDetailsFilter(paymentSharedStateTrackerMock) {
      @Override
      boolean remoteAssetChanged() {
        return true;
      }
    };

    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();

    assetDetailsFilter.nextState(request);
  }

  @Test
  public void nextStateWhenDenominationPresentWithFinalLength24() {
    this.assetDetailsFilter = new AssetDetailsFilter(paymentSharedStateTrackerMock) {
      @Override
      boolean remoteDenominationPresent() {
        return false;
      }
    };

    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();

    when(assetDetailsTrackerMock.getDestinationAccountDetails()).thenReturn(AccountDetails.builder()
      .denomination(Denominations.XRP_DROPS)
      .interledgerAddress(RECEIVER_ADDRESS.with("123456789123456789123456"))
      .build());

    SendState response = assetDetailsFilter.nextState(request);

    assertThat(response).isEqualTo(SendState.Ready);
    StreamFrame frame = request.requestFrames().get(0);
    assertThat(frame.streamFrameType()).isEqualTo(StreamFrameType.ConnectionNewAddress);
    assertThat(((ConnectionNewAddressFrame) frame).sourceAddress().get()).isEqualTo(SENDER_ADDRESS);
  }

  @Test
  public void nextStateWhenDenominationPresentWithFinalLengthNot24() {
    this.assetDetailsFilter = new AssetDetailsFilter(paymentSharedStateTrackerMock) {
      @Override
      boolean remoteDenominationPresent() {
        return false;
      }
    };

    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();

    when(assetDetailsTrackerMock.getDestinationAccountDetails()).thenReturn(AccountDetails.builder()
      .denomination(Denominations.XRP_DROPS)
      .interledgerAddress(RECEIVER_ADDRESS)
      .build());

    SendState response = assetDetailsFilter.nextState(request);

    assertThat(response).isEqualTo(SendState.Ready);
    StreamFrame frame = request.requestFrames().get(0);
    assertThat(frame.streamFrameType()).isEqualTo(StreamFrameType.ConnectionNewAddress);
    assertThat(((ConnectionNewAddressFrame) frame).sourceAddress()).isEmpty();
  }

  @Test
  public void nextStateRemoteKnowsOurAccount() {
    this.assetDetailsFilter = new AssetDetailsFilter(paymentSharedStateTrackerMock) {
      @Override
      boolean remoteKnowsOurAccount() {
        return false;
      }
    };

    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();

    when(assetDetailsTrackerMock.getDestinationAccountDetails()).thenReturn(AccountDetails.builder()
      .denomination(Denominations.XRP_DROPS)
      .interledgerAddress(RECEIVER_ADDRESS)
      .build());

    SendState response = assetDetailsFilter.nextState(request);

    assertThat(response).isEqualTo(SendState.Ready);
    assertThat(request.requestFrames().size()).isEqualTo(3);
    assertThat(request.requestFrames().stream()
      .filter(streamFrame -> streamFrame.streamFrameType() == StreamFrameType.StreamMoneyMax)
      .findFirst()
      .isPresent()
    ).isTrue();

    assertThat(request.requestFrames().stream()
      .filter(streamFrame -> streamFrame.streamFrameType() == StreamFrameType.ConnectionDataMax)
      .findFirst()
      .isPresent()
    ).isTrue();

    assertThat(request.requestFrames().stream()
      .filter(streamFrame -> streamFrame.streamFrameType() == StreamFrameType.ConnectionMaxStreamId)
      .findFirst()
      .isPresent()
    ).isTrue();
  }

  ////////////
  // doFilter
  ////////////

  @Test
  public void doFilterWhenNullStreamRequest() {
    expectedException.expect(NullPointerException.class);
    this.assetDetailsFilter.doFilter(null, mock(StreamPacketFilterChain.class));
  }

  @Test
  public void doFilterWhenNullFilterChain() {
    expectedException.expect(NullPointerException.class);
    this.assetDetailsFilter.doFilter(mock(StreamPacketRequest.class), null);
  }

  @Test
  public void doFilter() {
    StreamPacketRequest streamPacketRequestMock = mock(StreamPacketRequest.class);
    StreamPacketReply streamPacketReplyMock = mock(StreamPacketReply.class);

    StreamPacketFilterChain filterChainMock = mock(StreamPacketFilterChain.class);
    when(filterChainMock.doFilter(any())).thenReturn(streamPacketReplyMock);

    StreamPacketReply actual = this.assetDetailsFilter.doFilter(streamPacketRequestMock, filterChainMock);

    assertThat(actual).isEqualTo(streamPacketReplyMock);
    verify(assetDetailsTrackerMock).handleDestinationDetails(any());
  }
}