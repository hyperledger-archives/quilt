package org.interledger.stream.frames;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.stream.frames.StreamFrameConstants.CONNECTION_ASSET_DETAILS;
import static org.interledger.stream.frames.StreamFrameConstants.CONNECTION_CLOSE;
import static org.interledger.stream.frames.StreamFrameConstants.CONNECTION_DATA_BLOCKED;
import static org.interledger.stream.frames.StreamFrameConstants.CONNECTION_DATA_MAX;
import static org.interledger.stream.frames.StreamFrameConstants.CONNECTION_MAX_STREAM_ID;
import static org.interledger.stream.frames.StreamFrameConstants.CONNECTION_NEW_ADDRESS;
import static org.interledger.stream.frames.StreamFrameConstants.CONNECTION_STREAM_ID_BLOCKED;
import static org.interledger.stream.frames.StreamFrameConstants.STREAM_CLOSE;
import static org.interledger.stream.frames.StreamFrameConstants.STREAM_DATA;
import static org.interledger.stream.frames.StreamFrameConstants.STREAM_DATA_BLOCKED;
import static org.interledger.stream.frames.StreamFrameConstants.STREAM_DATA_MAX;
import static org.interledger.stream.frames.StreamFrameConstants.STREAM_MONEY;
import static org.interledger.stream.frames.StreamFrameConstants.STREAM_MONEY_BLOCKED;
import static org.interledger.stream.frames.StreamFrameConstants.STREAM_MONEY_MAX;

import org.interledger.core.InterledgerAddress;
import org.interledger.stream.Denomination;

import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedLong;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


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
    assertThat(Arrays.stream(StreamFrameType.values()).map(StreamFrameType::code).collect(Collectors.toList()))
      .isEqualTo(expected);
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
  public void connectionAssetDetailsFrame() {
    ConnectionAssetDetailsFrame frame = ConnectionAssetDetailsFrame.builder().sourceDenomination(
      Denomination.builder()
        .assetCode("dave and busters dollars")
        .assetScale((short) 1)
        .build())
      .build();
    assertThat(frame.streamFrameType()).isEqualTo(StreamFrameType.ConnectionAssetDetails);
    // make sure interface default method is exercised
    final ConnectionAssetDetailsFrame interfaceFrame = new ConnectionAssetDetailsFrame() {
      @Override
      public Denomination sourceDenomination() {
        return null;
      }
    };
    assertThat(interfaceFrame.streamFrameType()).isEqualTo(StreamFrameType.ConnectionAssetDetails);
  }

  @Test
  public void connectionCloseFrame() {
    ConnectionCloseFrame frame = ConnectionCloseFrame.builder()
      .errorMessage("too many cooks!")
      .errorCode(ErrorCodes.ApplicationError)
      .build();
    assertThat(frame.streamFrameType()).isEqualTo(StreamFrameType.ConnectionClose);
    // make sure interface default method is exercised
    final ConnectionCloseFrame interfaceFrame = new ConnectionCloseFrame() {
      @Override
      public ErrorCode errorCode() {
        return null;
      }

      @Override
      public Optional<String> errorMessage() {
        return Optional.empty();
      }
    };
    assertThat(interfaceFrame.streamFrameType()).isEqualTo(StreamFrameType.ConnectionClose);
  }

  @Test
  public void connectionDataBlockedFrame() {
    ConnectionDataBlockedFrame frame = ConnectionDataBlockedFrame.builder()
      .maxOffset(UnsignedLong.ZERO)
      .build();
    assertThat(frame.streamFrameType()).isEqualTo(StreamFrameType.ConnectionDataBlocked);
    // make sure interface default method is exercised
    final ConnectionDataBlockedFrame interfaceFrame = new ConnectionDataBlockedFrame() {
      @Override
      public UnsignedLong maxOffset() {
        return null;
      }
    };
    assertThat(interfaceFrame.streamFrameType()).isEqualTo(StreamFrameType.ConnectionDataBlocked);
  }

  @Test
  public void connectionDataMaxFrame() {
    ConnectionDataMaxFrame frame = ConnectionDataMaxFrame.builder()
      .maxOffset(UnsignedLong.ZERO)
      .build();
    assertThat(frame.streamFrameType()).isEqualTo(StreamFrameType.ConnectionDataMax);
    // make sure interface default method is exercised
    final ConnectionDataMaxFrame interfaceFrame = new ConnectionDataMaxFrame() {
      @Override
      public UnsignedLong maxOffset() {
        return null;
      }
    };
    assertThat(interfaceFrame.streamFrameType()).isEqualTo(StreamFrameType.ConnectionDataMax);
  }

  @Test
  public void connectionMaxStreamIdFrame() {
    ConnectionMaxStreamIdFrame frame = ConnectionMaxStreamIdFrame.builder()
      .maxStreamId(UnsignedLong.ZERO)
      .build();
    assertThat(frame.streamFrameType()).isEqualTo(StreamFrameType.ConnectionMaxStreamId);
    // make sure interface default method is exercised
    final ConnectionMaxStreamIdFrame interfaceFrame = new ConnectionMaxStreamIdFrame() {
      @Override
      public UnsignedLong maxStreamId() {
        return null;
      }
    };
    assertThat(interfaceFrame.streamFrameType()).isEqualTo(StreamFrameType.ConnectionMaxStreamId);
  }

  @Test
  public void connectionNewAddressFrame() {
    ConnectionNewAddressFrame frame = ConnectionNewAddressFrame.builder()
      .sourceAddress(InterledgerAddress.of("g.shenanigans"))
      .build();
    assertThat(frame.streamFrameType()).isEqualTo(StreamFrameType.ConnectionNewAddress);
    // make sure interface default method is exercised
    final ConnectionNewAddressFrame interfaceFrame = () -> Optional.empty();
    assertThat(interfaceFrame.streamFrameType()).isEqualTo(StreamFrameType.ConnectionNewAddress);
  }

  @Test
  public void connectionNewAddressFrameWithEmptyAddress() {
    ConnectionNewAddressFrame frame = ConnectionNewAddressFrame.builder().build();
    assertThat(frame.streamFrameType()).isEqualTo(StreamFrameType.ConnectionNewAddress);
    // make sure interface default method is exercised
    final ConnectionNewAddressFrame interfaceFrame = () -> Optional.empty();
    assertThat(interfaceFrame.streamFrameType()).isEqualTo(StreamFrameType.ConnectionNewAddress);
  }

  @Test
  public void connectionStreamIdBlockedFrame() {
    ConnectionStreamIdBlockedFrame frame = ConnectionStreamIdBlockedFrame.builder()
      .maxStreamId(UnsignedLong.ZERO)
      .build();
    assertThat(frame.streamFrameType()).isEqualTo(StreamFrameType.ConnectionStreamIdBlocked);
    // make sure interface default method is exercised
    final ConnectionStreamIdBlockedFrame interfaceFrame = new ConnectionStreamIdBlockedFrame() {
      @Override
      public UnsignedLong maxStreamId() {
        return null;
      }
    };
    assertThat(interfaceFrame.streamFrameType()).isEqualTo(StreamFrameType.ConnectionStreamIdBlocked);
  }

  @Test
  public void streamCloseFrame() {
    StreamCloseFrame frame = StreamCloseFrame.builder()
      .streamId(UnsignedLong.ZERO)
      .errorMessage("i'm all out of love")
      .errorCode(ErrorCodes.ApplicationError)
      .build();
    assertThat(frame.streamFrameType()).isEqualTo(StreamFrameType.StreamClose);
    // make sure interface default method is exercised
    final StreamCloseFrame interfaceFrame = new StreamCloseFrame() {
      @Override
      public UnsignedLong streamId() {
        return null;
      }

      @Override
      public ErrorCode errorCode() {
        return null;
      }
    };
    assertThat(interfaceFrame.streamFrameType()).isEqualTo(StreamFrameType.StreamClose);
  }

  @Test
  public void streamDataBlockedFrame() {
    StreamDataBlockedFrame frame = StreamDataBlockedFrame.builder()
      .maxOffset(UnsignedLong.ZERO)
      .streamId(UnsignedLong.ZERO)
      .build();
    assertThat(frame.streamFrameType()).isEqualTo(StreamFrameType.StreamDataBlocked);
    // make sure interface default method is exercised
    final StreamDataBlockedFrame interfaceFrame = new StreamDataBlockedFrame() {
      @Override
      public UnsignedLong streamId() {
        return null;
      }

      @Override
      public UnsignedLong maxOffset() {
        return null;
      }
    };
    assertThat(interfaceFrame.streamFrameType()).isEqualTo(StreamFrameType.StreamDataBlocked);
  }

  @Test
  public void streamDataFrame() {
    StreamDataFrame frame = StreamDataFrame.builder()
      .streamId(UnsignedLong.ZERO)
      .offset(UnsignedLong.ZERO)
      .data("ah man i lost the recipe for ice again" .getBytes())
      .build();
    assertThat(frame.streamFrameType()).isEqualTo(StreamFrameType.StreamData);
    // make sure interface default method is exercised
    final StreamDataFrame interfaceFrame = new StreamDataFrame() {
      @Override
      public UnsignedLong streamId() {
        return null;
      }

      @Override
      public UnsignedLong offset() {
        return null;
      }
    };
    assertThat(interfaceFrame.streamFrameType()).isEqualTo(StreamFrameType.StreamData);
  }

  @Test
  public void streamDataMaxFrame() {
    StreamDataMaxFrame frame = StreamDataMaxFrame.builder()
      .streamId(UnsignedLong.ZERO)
      .maxOffset(UnsignedLong.ZERO)
      .build();
    assertThat(frame.streamFrameType()).isEqualTo(StreamFrameType.StreamDataMax);
    // make sure interface default method is exercised
    final StreamDataMaxFrame interfaceFrame = new StreamDataMaxFrame() {
      @Override
      public UnsignedLong streamId() {
        return null;
      }

      @Override
      public UnsignedLong maxOffset() {
        return null;
      }
    };
    assertThat(interfaceFrame.streamFrameType()).isEqualTo(StreamFrameType.StreamDataMax);
  }

  @Test
  public void streamMoneyBlockedFrame() {
    StreamMoneyBlockedFrame frame = StreamMoneyBlockedFrame.builder()
      .streamId(UnsignedLong.ZERO)
      .sendMax(UnsignedLong.ZERO)
      .totalSent(UnsignedLong.ZERO)
      .build();
    assertThat(frame.streamFrameType()).isEqualTo(StreamFrameType.StreamMoneyBlocked);
    // make sure interface default method is exercised
    final StreamMoneyBlockedFrame interfaceFrame = new StreamMoneyBlockedFrame() {
      @Override
      public UnsignedLong streamId() {
        return null;
      }

      @Override
      public UnsignedLong sendMax() {
        return null;
      }
    };
    assertThat(interfaceFrame.streamFrameType()).isEqualTo(StreamFrameType.StreamMoneyBlocked);
  }

  @Test
  public void streamMoneyFrame() {
    StreamMoneyFrame frame = StreamMoneyFrame.builder()
      .streamId(UnsignedLong.ZERO)
      .shares(UnsignedLong.ZERO)
      .build();
    assertThat(frame.streamFrameType()).isEqualTo(StreamFrameType.StreamMoney);
    // make sure interface default method is exercised
    final StreamMoneyFrame interfaceFrame = new StreamMoneyFrame() {
      @Override
      public UnsignedLong streamId() {
        return null;
      }

      @Override
      public UnsignedLong shares() {
        return null;
      }
    };
    assertThat(interfaceFrame.streamFrameType()).isEqualTo(StreamFrameType.StreamMoney);
  }

  @Test
  public void streamMoneyMaxFrame() {
    StreamMoneyMaxFrame frame = StreamMoneyMaxFrame.builder()
      .totalReceived(UnsignedLong.ZERO)
      .streamId(UnsignedLong.ZERO)
      .receiveMax(UnsignedLong.ZERO)
      .build();
    assertThat(frame.streamFrameType()).isEqualTo(StreamFrameType.StreamMoneyMax);
    // make sure interface default method is exercised
    final StreamMoneyMaxFrame interfaceFrame = new StreamMoneyMaxFrame() {
      @Override
      public UnsignedLong streamId() {
        return null;
      }

      @Override
      public UnsignedLong receiveMax() {
        return null;
      }
    };
    assertThat(interfaceFrame.streamFrameType()).isEqualTo(StreamFrameType.StreamMoneyMax);

  }
}
