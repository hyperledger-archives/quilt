package org.interledger.stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPacketType;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.SharedSecret;
import org.interledger.fx.Denomination;
import org.interledger.stream.connection.StreamConnection;
import org.interledger.stream.crypto.StreamEncryptionUtils;
import org.interledger.stream.crypto.StreamPacketEncryptionService;
import org.interledger.stream.crypto.StreamSharedSecret;
import org.interledger.stream.frames.ConnectionAssetDetailsFrame;
import org.interledger.stream.frames.ConnectionCloseFrame;
import org.interledger.stream.frames.ConnectionNewAddressFrame;
import org.interledger.stream.frames.ErrorCodes;
import org.interledger.stream.frames.StreamCloseFrame;
import org.interledger.stream.frames.StreamFrameType;
import org.interledger.stream.frames.StreamMoneyFrame;
import org.interledger.stream.frames.StreamMoneyMaxFrame;

import com.google.common.io.BaseEncoding;
import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

/**
 * Unit tests for {@link StreamPacketUtils}.
 */
@SuppressWarnings("deprecation")
public class StreamPacketUtilsTest {

  private static final Denomination DENOMINATION = Denomination.builder()
    .assetScale((short) 9)
    .assetCode("XRP")
    .build();

  private static final InterledgerAddress ADDRESS = InterledgerAddress.of("test.foo");

  private static final StreamPacket JUST_CONNECTION_ASSET_DETAILS_FRAME =
    newPacketBuilder().addFrames(assetDetailsFrame(DENOMINATION)).build();

  private static final StreamPacket JUST_MONEY_FRAME =
    newPacketBuilder().addFrames(moneyFrame()).build();

  private static final StreamPacket JUST_MAX_MONEY_FRAME =
    newPacketBuilder().addFrames(maxMoneyFrame()).build();

  private static final StreamPacket JUST_STREAM_CLOSE_FRAME =
    newPacketBuilder().addFrames(streamCloseFrame()).build();

  private static final StreamPacket JUST_CONNECTION_CLOSE_FRAME =
    newPacketBuilder().addFrames(connectionCloseFrame()).build();

  private static final StreamPacket JUST_CONNECTION_NEW_ADDRESS_FRAME =
    newPacketBuilder().addFrames(connectionNewAddressFrame(ADDRESS)).build();

  private static final StreamPacket ALL_THE_FRAMES =
    newPacketBuilder().addFrames(
      streamCloseFrame(),
      connectionCloseFrame(),
      assetDetailsFrame(DENOMINATION),
      moneyFrame(),
      maxMoneyFrame(),
      connectionNewAddressFrame(ADDRESS)).build();

  private static final StreamPacket NO_FRAMES = newPacketBuilder().build();

  @Mock
  private StreamEncryptionUtils streamEncryptionUtilsMock;

  @Mock
  private StreamPacketEncryptionService streamPacketEncryptionServiceMock;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void unfulfillableCondition() {
    assertThat(StreamPacketUtils.unfulfillableCondition()).isNotNull();
  }

  //////////////////////////
  // generateFulfillableFulfillment
  //////////////////////////

  @SuppressWarnings("ConstantConditions")
  @Test
  public void generatedFulfillableFulfillmentNullSharedSecret() {
    expectedException.expect(NullPointerException.class);
    SharedSecret nullSharedSecret = null;
    StreamPacketUtils.generateFulfillableFulfillment(nullSharedSecret, new byte[32]);
  }

  @Test
  public void generatedFulfillableFulfillmentNullBytes() {
    expectedException.expect(NullPointerException.class);
    StreamPacketUtils.generateFulfillableFulfillment(SharedSecret.of(new byte[32]), null);
  }

  @Test
  public void generatedFulfillableFulfillmentWithInsufficientBytes() {
    assertThat(StreamPacketUtils.generateFulfillableFulfillment(SharedSecret.of(new byte[32]), new byte[0]))
      .isEqualTo(
        InterledgerFulfillment.of(BaseEncoding.base64().decode("uKsQg0EsVCm8gAJpXxoPjB6Z3BplNAxkeNyNRUluSAg="))
      );
  }

  @Test
  public void generatedFulfillableFulfillment() {
    assertThat(StreamPacketUtils.generateFulfillableFulfillment(SharedSecret.of(new byte[32]), new byte[32]))
      .isEqualTo(
        InterledgerFulfillment.of(BaseEncoding.base64().decode("IFL4ByJE+dN7FLiNxUyWs5bPLl0d6LcpI+hCIeXTaH4="))
      );
  }

  @Test
  public void generatedFulfillableFulfillmentWithNullStreamSharedSecret() {
    expectedException.expect(NullPointerException.class);
    StreamSharedSecret nullStreamSharedSecret = null;
    StreamPacketUtils.generateFulfillableFulfillment(nullStreamSharedSecret, new byte[32]);
  }

  @Test
  public void generatedFulfillableFulfillmentWithNullBytesStream() {
    expectedException.expect(NullPointerException.class);
    StreamPacketUtils.generateFulfillableFulfillment(StreamSharedSecret.of(new byte[32]), null);
  }

  @Test
  public void generatedFulfillableFulfillmentWithInsufficientBytesStream() {
    assertThat(StreamPacketUtils.generateFulfillableFulfillment(StreamSharedSecret.of(new byte[32]), new byte[0]))
      .isEqualTo(
        InterledgerFulfillment.of(BaseEncoding.base64().decode("uKsQg0EsVCm8gAJpXxoPjB6Z3BplNAxkeNyNRUluSAg="))
      );
  }

  @Test
  public void generatedFulfillableFulfillmentStream() {
    assertThat(StreamPacketUtils.generateFulfillableFulfillment(StreamSharedSecret.of(new byte[32]), new byte[32]))
      .isEqualTo(
        InterledgerFulfillment.of(BaseEncoding.base64().decode("IFL4ByJE+dN7FLiNxUyWs5bPLl0d6LcpI+hCIeXTaH4="))
      );
  }

  //////////////////////////
  // mapToStreamPacket (bytes)
  //////////////////////////

  @Test
  public void mapToStreamPacketWithNullPacketData() {
    expectedException.expect(NullPointerException.class);
    StreamPacketUtils.mapToStreamPacket(null, SharedSecret.of(new byte[32]), streamEncryptionUtilsMock);
  }

  @Test
  public void mapToStreamPacketWithNullSharedSecret() {
    expectedException.expect(NullPointerException.class);
    StreamPacketUtils.mapToStreamPacket(new byte[1], null, streamEncryptionUtilsMock);
  }

  @Test
  public void mapToStreamPacketWithNullEncryptionUtils() {
    expectedException.expect(NullPointerException.class);
    StreamEncryptionUtils nullStreamEncryptionUtils = null;
    StreamPacketUtils.mapToStreamPacket(new byte[1], SharedSecret.of(new byte[32]), nullStreamEncryptionUtils);
  }

  @Test
  public void mapToStreamPacket2WithNullPacketData() {
    expectedException.expect(NullPointerException.class);
    StreamPacketUtils.mapToStreamPacket(
      null, StreamSharedSecret.of(new byte[32]), mock(StreamPacketEncryptionService.class)
    );
  }

  @Test
  public void mapToStreamPacketWithNullStreamSharedSecret() {
    expectedException.expect(NullPointerException.class);
    StreamPacketUtils.mapToStreamPacket(new byte[1], null, mock(StreamPacketEncryptionService.class));
  }


  @Test
  public void mapToStreamPacketWithNullStreamPacketService() {
    expectedException.expect(NullPointerException.class);
    StreamPacketEncryptionService nullService = null;
    StreamPacketUtils.mapToStreamPacket(new byte[1], StreamSharedSecret.of(new byte[32]), nullService);
  }

  @Test
  public void mapToStreamPacketWithEmptyPacket() {
    assertThat(StreamPacketUtils.mapToStreamPacket(new byte[0], SharedSecret.of(new byte[32]),
      streamEncryptionUtilsMock))
      .isEmpty();
  }

  @Test
  public void mapToStreamPacketWhenPresent() {
    when(streamEncryptionUtilsMock.fromEncrypted(any(), any())).thenReturn(mock(StreamPacket.class));
    assertThat(StreamPacketUtils.mapToStreamPacket(new byte[1], SharedSecret.of(new byte[32]),
      streamEncryptionUtilsMock))
      .isPresent();
  }

  @Test
  public void mapToStreamPacketWhenException() {
    doThrow(new RuntimeException())
      .when(streamEncryptionUtilsMock)
      .fromEncrypted(any(), any());

    assertThat(StreamPacketUtils.mapToStreamPacket(new byte[1], SharedSecret.of(new byte[32]),
      streamEncryptionUtilsMock))
      .isEmpty();
  }

  @Test
  public void mapToStreamPacket2WithEmptyPacket() {
    assertThat(StreamPacketUtils.mapToStreamPacket(
      new byte[0], StreamSharedSecret.of(new byte[32]), streamPacketEncryptionServiceMock)
    ).isEmpty();
  }

  @Test
  public void mapToStreamPacket2WhenPresent() {
    when(streamPacketEncryptionServiceMock
      .fromEncrypted(Mockito.<StreamSharedSecret>any(), any()))
      .thenReturn(mock(StreamPacket.class));

    assertThat(StreamPacketUtils.mapToStreamPacket(
      new byte[1], StreamSharedSecret.of(new byte[32]), streamPacketEncryptionServiceMock)
    ).isPresent();
  }

  @Test
  public void mapToStreamPacket2WhenException() {
    doThrow(new RuntimeException())
      .when(streamPacketEncryptionServiceMock)
      .fromEncrypted(Mockito.<StreamSharedSecret>any(), any());

    assertThat(StreamPacketUtils.mapToStreamPacket(new byte[1], StreamSharedSecret.of(new byte[32]),
      streamPacketEncryptionServiceMock))
      .isEmpty();
  }

  //////////////////////////
  // mapToStreamPacket (packet)
  //////////////////////////

  @Test
  public void mapToStreamPacketFromPacketWhenEmpty() {
    InterledgerResponsePacket interledgerResponsePacket = mock(InterledgerResponsePacket.class);
    when(interledgerResponsePacket.typedData()).thenReturn(Optional.empty());
    assertThat(StreamPacketUtils.mapToStreamPacket(interledgerResponsePacket)).isEmpty();
  }

  @Test
  public void mapToStreamPacketFromPacketWhenPresent() {
    StreamPacket streamPacket = newPacketBuilder().build();
    InterledgerResponsePacket interledgerResponsePacket = mock(InterledgerResponsePacket.class);
    when(interledgerResponsePacket.typedData()).thenReturn(Optional.of(streamPacket));
    assertThat(StreamPacketUtils.mapToStreamPacket(interledgerResponsePacket)).isPresent();
  }

  //////////////////////////
  // findDenominationFromFrames
  //////////////////////////

  @Test
  public void findDenominationFromFrames() {
    assertThat(StreamPacketUtils.findDenominationFromFrames(JUST_STREAM_CLOSE_FRAME)).isEmpty();
    assertThat(StreamPacketUtils.findDenominationFromFrames(JUST_CONNECTION_CLOSE_FRAME)).isEmpty();
    assertThat(StreamPacketUtils.findDenominationFromFrames(JUST_CONNECTION_ASSET_DETAILS_FRAME))
      .hasValue(DENOMINATION);
    assertThat(StreamPacketUtils.findDenominationFromFrames(ALL_THE_FRAMES)).hasValue(DENOMINATION);
    assertThat(StreamPacketUtils.findDenominationFromFrames(JUST_MONEY_FRAME)).isEmpty();
    assertThat(StreamPacketUtils.findDenominationFromFrames(NO_FRAMES)).isEmpty();
  }

  //////////////////////////
  // findNewConnectionAddressFromFrames
  //////////////////////////

  @Test
  public void findNewConnectionAddressFromFrames() {
    assertThat(StreamPacketUtils.findNewConnectionAddressFromFrames(JUST_STREAM_CLOSE_FRAME)).isEmpty();
    assertThat(StreamPacketUtils.findNewConnectionAddressFromFrames(JUST_CONNECTION_NEW_ADDRESS_FRAME))
      .hasValue(ADDRESS);
    assertThat(StreamPacketUtils.findNewConnectionAddressFromFrames(ALL_THE_FRAMES)).hasValue(ADDRESS);
    assertThat(StreamPacketUtils.findNewConnectionAddressFromFrames(JUST_MONEY_FRAME)).isEmpty();
    assertThat(StreamPacketUtils.findNewConnectionAddressFromFrames(NO_FRAMES)).isEmpty();
  }

  //////////////////////////
  // hasCloseFrame
  //////////////////////////

  @Test
  public void hasCloseFrame() {
    assertThat(StreamPacketUtils.hasStreamCloseFrames(JUST_STREAM_CLOSE_FRAME)).isTrue();
    assertThat(StreamPacketUtils.hasStreamCloseFrames(JUST_CONNECTION_CLOSE_FRAME)).isTrue();
    assertThat(StreamPacketUtils.hasStreamCloseFrames(ALL_THE_FRAMES)).isTrue();
    assertThat(StreamPacketUtils.hasStreamCloseFrames(JUST_MONEY_FRAME)).isFalse();
    assertThat(StreamPacketUtils.hasStreamCloseFrames(NO_FRAMES)).isFalse();
  }

  //////////////////////////
  // findConnectionCloseFrame
  //////////////////////////

  @Test
  public void findConnectionCloseFrame() {
    assertThat(StreamPacketUtils.findConnectionCloseFrame(JUST_STREAM_CLOSE_FRAME)).isEmpty();
    assertThat(StreamPacketUtils.findConnectionCloseFrame(JUST_CONNECTION_CLOSE_FRAME)).isPresent();
    assertThat(StreamPacketUtils.findConnectionCloseFrame(ALL_THE_FRAMES)).isPresent();
    assertThat(StreamPacketUtils.findConnectionCloseFrame(JUST_MONEY_FRAME)).isEmpty();
    assertThat(StreamPacketUtils.findConnectionCloseFrame(NO_FRAMES)).isEmpty();
  }

  @Test
  public void findConnectionCloseFrameFromCollection() {
    assertThat(StreamPacketUtils.findConnectionCloseFrame(JUST_STREAM_CLOSE_FRAME.frames())).isEmpty();
    assertThat(StreamPacketUtils.findConnectionCloseFrame(JUST_CONNECTION_CLOSE_FRAME.frames())).isPresent();
    assertThat(StreamPacketUtils.findConnectionCloseFrame(ALL_THE_FRAMES.frames())).isPresent();
    assertThat(StreamPacketUtils.findConnectionCloseFrame(JUST_MONEY_FRAME.frames())).isEmpty();
    assertThat(StreamPacketUtils.findConnectionCloseFrame(NO_FRAMES.frames())).isEmpty();
  }

  //////////////////////////
  // findStreamCloseFrame
  //////////////////////////

  @Test
  public void findStreamCloseFrame() {
    assertThat(StreamPacketUtils.findStreamCloseFrame(JUST_STREAM_CLOSE_FRAME)).isPresent();
    assertThat(StreamPacketUtils.findStreamCloseFrame(JUST_CONNECTION_CLOSE_FRAME)).isEmpty();
    assertThat(StreamPacketUtils.findStreamCloseFrame(ALL_THE_FRAMES)).isPresent();
    assertThat(StreamPacketUtils.findStreamCloseFrame(JUST_MONEY_FRAME)).isEmpty();
    assertThat(StreamPacketUtils.findStreamCloseFrame(NO_FRAMES)).isEmpty();
  }

  @Test
  public void findStreamCloseFrameFromCollection() {
    assertThat(StreamPacketUtils.findStreamCloseFrame(JUST_STREAM_CLOSE_FRAME.frames())).isPresent();
    assertThat(StreamPacketUtils.findStreamCloseFrame(JUST_CONNECTION_CLOSE_FRAME.frames())).isEmpty();
    assertThat(StreamPacketUtils.findStreamCloseFrame(ALL_THE_FRAMES.frames())).isPresent();
    assertThat(StreamPacketUtils.findStreamCloseFrame(JUST_MONEY_FRAME.frames())).isEmpty();
    assertThat(StreamPacketUtils.findStreamCloseFrame(NO_FRAMES.frames())).isEmpty();
  }

  //////////////////////////
  // findConnectionAssetDetailsFrame
  //////////////////////////

  @Test
  public void findConnectionAssetDetailsFrame() {
    assertThat(StreamPacketUtils.findConnectionAssetDetailsFrame(JUST_STREAM_CLOSE_FRAME)).isEmpty();
    assertThat(StreamPacketUtils.findConnectionAssetDetailsFrame(JUST_CONNECTION_CLOSE_FRAME)).isEmpty();
    assertThat(StreamPacketUtils.findConnectionAssetDetailsFrame(ALL_THE_FRAMES)).isPresent();
    assertThat(StreamPacketUtils.findConnectionAssetDetailsFrame(JUST_MONEY_FRAME)).isEmpty();
    assertThat(StreamPacketUtils.findConnectionAssetDetailsFrame(NO_FRAMES)).isEmpty();
  }

  @Test
  public void findConnectionAssetDetailsFrameFromCollection() {
    assertThat(StreamPacketUtils.findConnectionAssetDetailsFrame(JUST_STREAM_CLOSE_FRAME.frames())).isEmpty();
    assertThat(StreamPacketUtils.findConnectionAssetDetailsFrame(JUST_CONNECTION_CLOSE_FRAME.frames())).isEmpty();
    assertThat(StreamPacketUtils.findConnectionAssetDetailsFrame(ALL_THE_FRAMES.frames())).isPresent();
    assertThat(StreamPacketUtils.findConnectionAssetDetailsFrame(JUST_MONEY_FRAME.frames())).isEmpty();
    assertThat(StreamPacketUtils.findConnectionAssetDetailsFrame(NO_FRAMES.frames())).isEmpty();
  }

  @Test
  public void countConnectionAssetDetailsFrameFromCollection() {
    StreamPacket streamPacket = newPacketBuilder()
      .frames(JUST_STREAM_CLOSE_FRAME.frames())
      .build();
    assertThat(StreamPacketUtils.countConnectionAssetDetailsFrame(streamPacket)).isEqualTo(0);

    streamPacket = newPacketBuilder()
      .frames(JUST_CONNECTION_CLOSE_FRAME.frames())
      .build();
    assertThat(StreamPacketUtils.countConnectionAssetDetailsFrame(streamPacket)).isEqualTo(0);

    streamPacket = newPacketBuilder()
      .frames(JUST_MONEY_FRAME.frames())
      .build();
    assertThat(StreamPacketUtils.countConnectionAssetDetailsFrame(streamPacket)).isEqualTo(0);

    streamPacket = newPacketBuilder()
      .frames(ALL_THE_FRAMES.frames())
      .build();
    assertThat(StreamPacketUtils.countConnectionAssetDetailsFrame(streamPacket)).isEqualTo(1);

    streamPacket = newPacketBuilder()
      .frames(NO_FRAMES.frames())
      .build();
    assertThat(StreamPacketUtils.countConnectionAssetDetailsFrame(streamPacket)).isEqualTo(0);
  }

  //////////////////////////
  // findStreamMaxMoneyFrames
  //////////////////////////

  @Test
  public void findStreamMaxMoneyFrames() {
    assertThat(StreamPacketUtils.findStreamMaxMoneyFrames(JUST_STREAM_CLOSE_FRAME).size()).isEqualTo(0);
    assertThat(StreamPacketUtils.findStreamMaxMoneyFrames(JUST_CONNECTION_CLOSE_FRAME).size()).isEqualTo(0);
    assertThat(StreamPacketUtils.findStreamMaxMoneyFrames(ALL_THE_FRAMES).size()).isEqualTo(1);
    assertThat(StreamPacketUtils.findStreamMaxMoneyFrames(JUST_MONEY_FRAME).size()).isEqualTo(0);
    assertThat(StreamPacketUtils.findStreamMaxMoneyFrames(JUST_MAX_MONEY_FRAME).size()).isEqualTo(1);
    assertThat(StreamPacketUtils.findStreamMaxMoneyFrames(NO_FRAMES).size()).isEqualTo(0);
  }

  @Test
  public void findStreamMaxMoneyFramesFromCollection() {
    assertThat(StreamPacketUtils.findStreamMaxMoneyFrames(JUST_STREAM_CLOSE_FRAME.frames()).size()).isEqualTo(0);
    assertThat(StreamPacketUtils.findStreamMaxMoneyFrames(JUST_CONNECTION_CLOSE_FRAME.frames()).size()).isEqualTo(0);
    assertThat(StreamPacketUtils.findStreamMaxMoneyFrames(ALL_THE_FRAMES.frames()).size()).isEqualTo(1);
    assertThat(StreamPacketUtils.findStreamMaxMoneyFrames(JUST_MONEY_FRAME.frames()).size()).isEqualTo(0);
    assertThat(StreamPacketUtils.findStreamMaxMoneyFrames(JUST_MAX_MONEY_FRAME.frames()).size()).isEqualTo(1);
    assertThat(StreamPacketUtils.findStreamMaxMoneyFrames(NO_FRAMES.frames()).size()).isEqualTo(0);
  }

  //////////////////////////
  // hasAuthenticStreamPacket
  //////////////////////////

  @Test
  public void hasAuthenticStreamPacket() {
    InterledgerResponsePacket responsePacketMock = mock(InterledgerResponsePacket.class);
    StreamPacket streamPacketMock = mock(StreamPacket.class);
    when(responsePacketMock.typedData()).thenReturn(Optional.of(streamPacketMock));
    assertThat(StreamPacketUtils.hasAuthenticStreamPacket(responsePacketMock)).isTrue();
  }

  @Test
  public void hasAuthenticStreamPacketWhenEmpty() {
    InterledgerResponsePacket responsePacketMock = mock(InterledgerResponsePacket.class);
    when(responsePacketMock.typedData()).thenReturn(Optional.empty());
    assertThat(StreamPacketUtils.hasAuthenticStreamPacket(responsePacketMock)).isFalse();
  }

  //////////////////////////
  // constructPacketToCloseStream
//////////////////////////

  @Test
  public void constructPacketToCloseStreamWithNullStreamConnection() {
    expectedException.expect(NullPointerException.class);
    StreamPacketUtils.constructPacketToCloseStream(null, streamPacketEncryptionServiceMock);
  }

  @Test
  public void constructPacketToCloseStreamWithNullService() {
    expectedException.expect(NullPointerException.class);
    StreamPacketUtils.constructPacketToCloseStream(mock(StreamConnection.class), null);
  }

  @Test
  public void constructPacketToCloseStream() {
    StreamConnection streamConnectionMock = mock(StreamConnection.class);
    when(streamConnectionMock.nextSequence()).thenReturn(UnsignedLong.ONE);
    when(streamConnectionMock.getDestinationAddress()).thenReturn(InterledgerAddress.of("example.dest"));
    when(streamPacketEncryptionServiceMock.toEncrypted(Mockito.<StreamSharedSecret>any(), any()))
      .thenReturn(new byte[1]);

    final InterledgerPreparePacket packet = StreamPacketUtils
      .constructPacketToCloseStream(streamConnectionMock, streamPacketEncryptionServiceMock);

    assertThat(packet.getDestination()).isEqualTo(InterledgerAddress.of("example.dest"));
    assertThat(packet.getAmount()).isEqualTo(UnsignedLong.ZERO);
    assertThat(packet.getExecutionCondition()).isNotNull();
    assertThat(packet.getExpiresAt()).isNotNull();
    // StreamPacket data.
    assertThat(packet.getData()).hasSize(1);
    assertThat(((StreamPacket) packet.typedData().get()).sequenceIsSafeForSingleSharedSecret()).isTrue();
    assertThat(((StreamPacket) packet.typedData().get()).prepareAmount()).isEqualTo(UnsignedLong.ZERO);
    assertThat(((StreamPacket) packet.typedData().get()).sequence()).isEqualTo(UnsignedLong.ONE);
    assertThat(((StreamPacket) packet.typedData().get()).version()).isEqualTo((short) 1);
    assertThat(((StreamPacket) packet.typedData().get()).frames().size()).isEqualTo(2);
    assertThat(((StreamPacket) packet.typedData().get()).frames().get(0).streamFrameType())
      .isEqualTo(StreamFrameType.StreamClose);
    assertThat(((StreamPacket) packet.typedData().get()).frames().get(1).streamFrameType())
      .isEqualTo(StreamFrameType.ConnectionClose);
  }

  //////////////////
  // private helpers
  //////////////////

  private static StreamPacketBuilder newPacketBuilder() {
    return StreamPacket.builder()
      .prepareAmount(UnsignedLong.ONE)
      .sequence(UnsignedLong.ONE)
      .interledgerPacketType(InterledgerPacketType.PREPARE);
  }

  private static StreamMoneyFrame moneyFrame() {
    return StreamMoneyFrame.builder()
      .shares(UnsignedLong.ONE)
      .streamId(UnsignedLong.ONE)
      .build();
  }

  private static StreamMoneyMaxFrame maxMoneyFrame() {
    return StreamMoneyMaxFrame.builder()
      .streamId(UnsignedLong.ONE)
      .receiveMax(UnsignedLong.MAX_VALUE)
      .totalReceived(UnsignedLong.ONE)
      .build();
  }

  private static ConnectionNewAddressFrame connectionNewAddressFrame(InterledgerAddress address) {
    return ConnectionNewAddressFrame.builder().sourceAddress(address)
      .build();
  }

  private static ConnectionAssetDetailsFrame assetDetailsFrame(Denomination denomination) {
    return ConnectionAssetDetailsFrame.builder()
      .sourceDenomination(org.interledger.stream.Denomination.builder()
        .assetCode(denomination.assetCode())
        .assetScale(denomination.assetScale())
        .build()
      )
      .build();
  }

  private static StreamCloseFrame streamCloseFrame() {
    return StreamCloseFrame.builder()
      .streamId(UnsignedLong.ONE)
      .errorCode(ErrorCodes.NoError)
      .build();
  }

  private static ConnectionCloseFrame connectionCloseFrame() {
    return ConnectionCloseFrame.builder()
      .errorCode(ErrorCodes.UnsupportedError)
      .errorMessage("error")
      .build();
  }

}