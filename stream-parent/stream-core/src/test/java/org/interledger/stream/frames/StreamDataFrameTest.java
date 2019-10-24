package org.interledger.stream.frames;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

public class StreamDataFrameTest {

  @Test
  public void emptyData() {
    StreamDataFrame frame = StreamDataFrame.builder()
        .offset(UnsignedLong.ZERO)
        .streamId(UnsignedLong.ZERO)
        .build();
    assertThat(frame.data()).isEqualTo(new byte[0]);
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
    assertThat(interfaceFrame.data()).isEqualTo(new byte[0]);
  }
}
