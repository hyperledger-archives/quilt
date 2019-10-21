package org.interledger.stream.frames;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

public class StreamMoneyBlockedFrameTest {

  @Test
  public void zero() {
    StreamMoneyBlockedFrame frame = StreamMoneyBlockedFrame.builder()
        .sendMax(UnsignedLong.ZERO)
        .streamId(UnsignedLong.ZERO)
        .build();
    assertThat(frame.totalSent()).isEqualTo(UnsignedLong.ZERO);

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
    assertThat(interfaceFrame.totalSent()).isEqualTo(UnsignedLong.ZERO);
  }
}
