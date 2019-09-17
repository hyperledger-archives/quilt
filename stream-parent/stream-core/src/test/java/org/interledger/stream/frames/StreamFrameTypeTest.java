package org.interledger.stream.frames;

import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedLong;
import org.interledger.core.InterledgerAddress;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.stream.frames.StreamFrameConstants.*;


public class StreamFrameTypeTest {

  private final List<Short> expected = Lists.newArrayList(
    CONNECTION_CLOSE,
    CONNECTION_NEW_ADDRESS,
    CONNECTION_DATA_MAX,
    CONNECTION_DATA_BLOCKED,
    CONNECTION_MAX_STREAM_ID,
    CONNECTION_STREAM_ID_BLOCKED,
    CONNECTION_ASSET_DETAILS,
    STREAM_CLOSE,
    STREAM_MONEY,
    STREAM_MONEY_MAX,
    STREAM_MONEY_BLOCKED,
    STREAM_DATA,
    STREAM_DATA_MAX,
    STREAM_DATA_BLOCKED
  );

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void fromCode() {
    assertThat(expected.stream().map(StreamFrameType::fromCode).collect(Collectors.toList()))
        .isEqualTo(Arrays.asList(StreamFrameType.values()));
  }

  @Test
  public void getCode() {
    assertThat(Arrays.stream(StreamFrameType.values()).map(StreamFrameType::code).collect(Collectors.toList())).isEqualTo(expected);
  }

  @Test
  public void fromCodeGetSad() {
    expectedException.expect(IllegalArgumentException.class);
    StreamFrameType.fromCode((short) 0x17);
  }

  @Test
  public void enforceHexValues() {
    assertThat(expected).containsExactly(
        (short) 0x01,
        (short) 0x02,
        (short) 0x03,
        (short) 0x04,
        (short) 0x05,
        (short) 0x06,
        (short) 0x07,
        (short) 0x10,
        (short) 0x11,
        (short) 0x12,
        (short) 0x13,
        (short) 0x14,
        (short) 0x15,
        (short) 0x16
    );
  }

  @Test
  public void connectionAssetDetailsFrame() throws Exception {
    ConnectionAssetDetailsFrame frame = ConnectionAssetDetailsFrame.builder()
        .sourceAssetCode("dave and busters dollars")
        .sourceAssetScale((short) 1)
        .build();
    assertThat(frame.streamFrameType()).isEqualTo(StreamFrameType.ConnectionAssetDetails);
  }

  @Test
  public void connectionCloseFrame() throws Exception {
    ConnectionCloseFrame frame = ConnectionCloseFrame.builder()
        .errorMessage("too many cooks!")
        .errorCode(ErrorCode.ApplicationError)
        .build();
    assertThat(frame.streamFrameType()).isEqualTo(StreamFrameType.ConnectionClose);
  }

  @Test
  public void connectionDataBlockedFrame() throws Exception {
    ConnectionDataBlockedFrame frame = ConnectionDataBlockedFrame.builder()
        .maxOffset(UnsignedLong.ZERO)
        .build();
    assertThat(frame.streamFrameType()).isEqualTo(StreamFrameType.ConnectionDataBlocked);
  }

  @Test
  public void connectionDataMaxFrame() throws Exception {
    ConnectionDataMaxFrame frame = ConnectionDataMaxFrame.builder()
        .maxOffset(UnsignedLong.ZERO)
        .build();
    assertThat(frame.streamFrameType()).isEqualTo(StreamFrameType.ConnectionDataMax);
  }

  @Test
  public void connectionMaxStreamIdFrame() throws Exception {
    ConnectionMaxStreamIdFrame frame = ConnectionMaxStreamIdFrame.builder()
        .maxStreamId(UnsignedLong.ZERO)
        .build();
    assertThat(frame.streamFrameType()).isEqualTo(StreamFrameType.ConnectionMaxStreamId);
  }

  @Test
  public void connectionNewAddressFrame() throws Exception {
    ConnectionNewAddressFrame frame = ConnectionNewAddressFrame.builder()
        .sourceAddress(InterledgerAddress.of("g.shenanigans"))
        .build();
    assertThat(frame.streamFrameType()).isEqualTo(StreamFrameType.ConnectionNewAddress);
  }

  @Test
  public void connectionStreamIdBlockedFrame() throws Exception {
    ConnectionStreamIdBlockedFrame frame = ConnectionStreamIdBlockedFrame.builder()
        .maxStreamId(UnsignedLong.ZERO)
        .build();
    assertThat(frame.streamFrameType()).isEqualTo(StreamFrameType.ConnectionStreamIdBlocked);
  }

  @Test
  public void streamCloseFrame() throws Exception {
    StreamCloseFrame frame = StreamCloseFrame.builder()
        .streamId(UnsignedLong.ZERO)
        .errorMessage("i'm all out of love")
        .errorCode(ErrorCode.ApplicationError)
        .build();
    assertThat(frame.streamFrameType()).isEqualTo(StreamFrameType.StreamClose);
  }

  @Test
  public void streamDataBlockedFrame() throws Exception {
    StreamDataBlockedFrame frame = StreamDataBlockedFrame.builder()
        .maxOffset(UnsignedLong.ZERO)
        .streamId(UnsignedLong.ZERO)
        .build();
    assertThat(frame.streamFrameType()).isEqualTo(StreamFrameType.StreamDataBlocked);
  }

  @Test
  public void streamDataFrame() throws Exception {
    StreamDataFrame frame = StreamDataFrame.builder()
        .streamId(UnsignedLong.ZERO)
        .offset(UnsignedLong.ZERO)
        .data("ah man i lost the recipe for ice again".getBytes())
        .build();
    assertThat(frame.streamFrameType()).isEqualTo(StreamFrameType.StreamData);
  }

  @Test
  public void streamDataMaxFrame() throws Exception  {
    StreamDataMaxFrame frame = StreamDataMaxFrame.builder()
        .streamId(UnsignedLong.ZERO)
        .maxOffset(UnsignedLong.ZERO)
        .build();
    assertThat(frame.streamFrameType()).isEqualTo(StreamFrameType.StreamDataMax);
  }

  @Test
  public void streamMoneyBlockedFrame() throws Exception {
    StreamMoneyBlockedFrame frame = StreamMoneyBlockedFrame.builder()
        .streamId(UnsignedLong.ZERO)
        .sendMax(UnsignedLong.ZERO)
        .totalSent(UnsignedLong.ZERO)
        .build();
    assertThat(frame.streamFrameType()).isEqualTo(StreamFrameType.StreamMoneyBlocked);
  }

  @Test
  public void streamMoneyFrame() throws Exception {
    StreamMoneyFrame frame = StreamMoneyFrame.builder()
        .streamId(UnsignedLong.ZERO)
        .shares(UnsignedLong.ZERO)
        .build();
    assertThat(frame.streamFrameType()).isEqualTo(StreamFrameType.StreamMoney);
  }

  @Test
  public void streamMoneyMaxFrame() throws Exception {
    StreamMoneyMaxFrame frame = StreamMoneyMaxFrame.builder()
        .totalReceived(UnsignedLong.ZERO)
        .streamId(UnsignedLong.ZERO)
        .receiveMax(UnsignedLong.ZERO)
        .build();
    assertThat(frame.streamFrameType()).isEqualTo(StreamFrameType.StreamMoneyMax);
  }
}
