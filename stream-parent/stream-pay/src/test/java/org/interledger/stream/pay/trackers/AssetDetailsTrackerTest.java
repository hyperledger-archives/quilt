package org.interledger.stream.pay.trackers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.stream.pay.StreamPayerExceptionMatcher.hasSendState;
import static org.interledger.stream.pay.model.SendState.DestinationAssetConflict;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPacketType;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.fx.Denominations;
import org.interledger.stream.StreamPacket;
import org.interledger.stream.frames.ConnectionAssetDetailsFrame;
import org.interledger.stream.model.AccountDetails;
import org.interledger.stream.pay.exceptions.StreamPayerException;
import org.interledger.stream.pay.model.StreamPacketReply;

import com.google.common.primitives.UnsignedLong;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link AssetDetailsTracker}.
 */
@SuppressWarnings( {"checkstyle:MissingJavadocMethod", "OptionalGetWithoutIsPresent"})
public class AssetDetailsTrackerTest {

  private static final InterledgerAddress RECEIVER_ADDRESS = InterledgerAddress.of("example.destination");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private AccountDetails sourceAccountDetails;

  private InterledgerAddress destinationAddress;

  private AssetDetailsTracker assetDetailsTracker;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    this.sourceAccountDetails = AccountDetails.builder()
      .interledgerAddress(InterledgerAddress.of("example.source"))
      .denomination(Denominations.XRP_DROPS)
      .build();
    this.destinationAddress = InterledgerAddress.of("example.destination");

    this.assetDetailsTracker = new AssetDetailsTracker(sourceAccountDetails, destinationAddress);
  }

  //////////////////////////
  // Constructor
  //////////////////////////

  @Test
  public void nullSourceAccountDetails() {
    expectedException.expect(NullPointerException.class);
    new AssetDetailsTracker(null, destinationAddress);
  }

  @Test
  public void nullDestinationAddress() {
    expectedException.expect(NullPointerException.class);
    new AssetDetailsTracker(sourceAccountDetails, null);
  }

  //////////////////////////
  // getSourceAccountDetails
  //////////////////////////

  @Test
  public void getSourceAccountDetails() {
    assertThat(assetDetailsTracker.getSourceAccountDetails()).isEqualTo(sourceAccountDetails);
  }

  //////////////////////////
  // getDestinationAccountDetails
  //////////////////////////

  @Test
  public void getDestinationAccountDetails() {
    assertThat(assetDetailsTracker.getDestinationAccountDetails().interledgerAddress()).isEqualTo(destinationAddress);
  }

  //////////////////////////
  // handleDestinationDetails
  //////////////////////////

  @Test
  public void handleDestinationDetailsWithNullReply() {
    expectedException.expect(NullPointerException.class);
    assetDetailsTracker.handleDestinationDetails(null);
  }

  @Test
  public void handleDestinationDetailsWithAuthenticReply() {
    StreamPacketReply streamPacketReply = mock(StreamPacketReply.class);
    when(streamPacketReply.isAuthentic()).thenReturn(true);
    assetDetailsTracker.handleDestinationDetails(streamPacketReply);

    assertThat(assetDetailsTracker.getRemoteKnowsOurAccount()).isTrue();
  }

  @Test
  public void handleDestinationDetailsWithInAuthenticReply() {
    StreamPacketReply streamPacketReply = mock(StreamPacketReply.class);
    when(streamPacketReply.isAuthentic()).thenReturn(false);
    assetDetailsTracker.handleDestinationDetails(streamPacketReply);

    assertThat(assetDetailsTracker.getRemoteKnowsOurAccount()).isFalse();
  }

  @Test
  public void handleDestinationDetailsWithDestinationAssetConflict() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(DestinationAssetConflict));
    StreamPacketReply streamPacketReply = StreamPacketReply.builder()
      .interledgerResponsePacket(InterledgerRejectPacket.builder()
        .code(InterledgerErrorCode.T04_INSUFFICIENT_LIQUIDITY)
        .typedData(StreamPacket.builder()
          .prepareAmount(UnsignedLong.ONE)
          .sequence(UnsignedLong.ONE)
          .interledgerPacketType(InterledgerPacketType.REJECT)
          .frames(Lists.newArrayList(
            ConnectionAssetDetailsFrame.builder()
              .sourceDenomination(org.interledger.stream.Denomination.from(Denominations.EUR_CENTS))
              .build(),
            ConnectionAssetDetailsFrame.builder()
              .sourceDenomination(org.interledger.stream.Denomination.from(Denominations.EUR_CENTS))
              .build()
          ))
          .build())
        .build())
      .build();

    assetDetailsTracker.handleDestinationDetails(streamPacketReply);
  }

  @Test
  public void handleDestinationDetailsWithNoDenomination() {
    StreamPacketReply streamPacketReply = StreamPacketReply.builder()
      .interledgerResponsePacket(InterledgerRejectPacket.builder()
        .code(InterledgerErrorCode.T04_INSUFFICIENT_LIQUIDITY)
        .typedData(StreamPacket.builder()
          .prepareAmount(UnsignedLong.ONE)
          .sequence(UnsignedLong.ONE)
          .interledgerPacketType(InterledgerPacketType.REJECT)
          .frames(Lists.newArrayList(
            ConnectionAssetDetailsFrame.builder()
              .sourceDenomination(org.interledger.stream.Denomination.from(Denominations.EUR_CENTS))
              .build()
          ))
          .build())
        .build())
      .build();

    assetDetailsTracker.handleDestinationDetails(streamPacketReply);

    assertThat(assetDetailsTracker.getRemoteKnowsOurAccount()).isTrue();
    assertThat(assetDetailsTracker.getDestinationAccountDetails().interledgerAddress()).isEqualTo(RECEIVER_ADDRESS);
    assertThat(assetDetailsTracker.getDestinationAccountDetails().denomination().get())
      .isEqualTo(Denominations.EUR_CENTS);
  }

  @Test
  public void handleDestinationDetailsWithDenominationNotEqualToFrame() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(DestinationAssetConflict));

    StreamPacketReply streamPacketReply = StreamPacketReply.builder()
      .interledgerResponsePacket(InterledgerRejectPacket.builder()
        .code(InterledgerErrorCode.T04_INSUFFICIENT_LIQUIDITY)
        .typedData(StreamPacket.builder()
          .prepareAmount(UnsignedLong.ONE)
          .sequence(UnsignedLong.ONE)
          .interledgerPacketType(InterledgerPacketType.REJECT)
          .frames(Lists.newArrayList(
            ConnectionAssetDetailsFrame.builder()
              .sourceDenomination(org.interledger.stream.Denomination.from(Denominations.EUR_CENTS))
              .build()
          ))
          .build())
        .build())
      .build();

    // This sets the denomination
    assetDetailsTracker.handleDestinationDetails(streamPacketReply);
    assertThat(assetDetailsTracker.getRemoteKnowsOurAccount()).isTrue();
    assertThat(assetDetailsTracker.getDestinationAccountDetails().interledgerAddress()).isEqualTo(RECEIVER_ADDRESS);
    assertThat(assetDetailsTracker.getDestinationAccountDetails().denomination().get())
      .isEqualTo(Denominations.EUR_CENTS);

    // This changes the value, which is an error.
    streamPacketReply = StreamPacketReply.builder()
      .interledgerResponsePacket(InterledgerRejectPacket.builder()
        .code(InterledgerErrorCode.T04_INSUFFICIENT_LIQUIDITY)
        .typedData(StreamPacket.builder()
          .prepareAmount(UnsignedLong.ONE)
          .sequence(UnsignedLong.ONE)
          .interledgerPacketType(InterledgerPacketType.REJECT)
          .frames(Lists.newArrayList(
            ConnectionAssetDetailsFrame.builder()
              .sourceDenomination(org.interledger.stream.Denomination.from(Denominations.USD))
              .build()
          ))
          .build())
        .build())
      .build();

    // This sets the denomination
    assetDetailsTracker.handleDestinationDetails(streamPacketReply);
  }

  @Test
  public void handleDestinationDetailsWithDenominationEqualToFrame() {
    StreamPacketReply streamPacketReply = StreamPacketReply.builder()
      .interledgerResponsePacket(InterledgerRejectPacket.builder()
        .code(InterledgerErrorCode.T04_INSUFFICIENT_LIQUIDITY)
        .typedData(StreamPacket.builder()
          .prepareAmount(UnsignedLong.ONE)
          .sequence(UnsignedLong.ONE)
          .interledgerPacketType(InterledgerPacketType.REJECT)
          .frames(Lists.newArrayList(
            ConnectionAssetDetailsFrame.builder()
              .sourceDenomination(org.interledger.stream.Denomination.from(Denominations.EUR_CENTS))
              .build()
          ))
          .build())
        .build())
      .build();

    // This sets the denomination
    assetDetailsTracker.handleDestinationDetails(streamPacketReply);
    assertThat(assetDetailsTracker.getRemoteKnowsOurAccount()).isTrue();
    assertThat(assetDetailsTracker.getDestinationAccountDetails().interledgerAddress()).isEqualTo(RECEIVER_ADDRESS);
    assertThat(assetDetailsTracker.getDestinationAccountDetails().denomination().get())
      .isEqualTo(Denominations.EUR_CENTS);

    // This changes the value, which is an error.
    streamPacketReply = StreamPacketReply.builder()
      .interledgerResponsePacket(InterledgerRejectPacket.builder()
        .code(InterledgerErrorCode.T04_INSUFFICIENT_LIQUIDITY)
        .typedData(StreamPacket.builder()
          .prepareAmount(UnsignedLong.ONE)
          .sequence(UnsignedLong.ONE)
          .interledgerPacketType(InterledgerPacketType.REJECT)
          .frames(Lists.newArrayList(
            ConnectionAssetDetailsFrame.builder()
              .sourceDenomination(org.interledger.stream.Denomination.from(Denominations.EUR_CENTS))
              .build()
          ))
          .build())
        .build())
      .build();

    // This sets the denomination
    assetDetailsTracker.handleDestinationDetails(streamPacketReply);
    assertThat(assetDetailsTracker.getRemoteKnowsOurAccount()).isTrue();
    assertThat(assetDetailsTracker.getDestinationAccountDetails().interledgerAddress()).isEqualTo(RECEIVER_ADDRESS);
    assertThat(assetDetailsTracker.getDestinationAccountDetails().denomination().get())
      .isEqualTo(Denominations.EUR_CENTS);
  }
}