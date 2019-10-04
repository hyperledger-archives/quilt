package org.interledger.stream.frames;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

public class StreamMoneyMaxFrameTest {

  @Test
  public void zero() {
    StreamMoneyMaxFrame frame = StreamMoneyMaxFrame.builder()
        .receiveMax(UnsignedLong.ZERO)
        .streamId(UnsignedLong.ZERO)
        .build();
    assertThat(frame.totalReceived()).isEqualTo(UnsignedLong.ZERO);
    assertThat(spy(StreamMoneyMaxFrame.class).totalReceived()).isEqualTo(UnsignedLong.ZERO);
  }
}
