package org.interledger.stream.frames;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

public class StreamDataFrameTest {

  @Test
  public void emptyData() {
    StreamDataFrame frame = StreamDataFrame.builder()
        .offset(UnsignedLong.ZERO)
        .streamId(UnsignedLong.ZERO)
        .build();
    assertThat(frame.data()).isEqualTo(new byte[0]);
    // make sure interface default method is exercised
    assertThat(spy(StreamDataFrame.class).data()).isEqualTo(new byte[0]);
  }
}
