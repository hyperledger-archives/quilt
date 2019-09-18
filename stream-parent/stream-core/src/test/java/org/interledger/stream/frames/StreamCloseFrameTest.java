package org.interledger.stream.frames;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

public class StreamCloseFrameTest {

  @Test
  public void emptyMessage() {
    StreamCloseFrame frame = StreamCloseFrame.builder()
        .streamId(UnsignedLong.ZERO)
        .errorCode(ErrorCode.NoError)
        .build();
    assertThat(frame.errorMessage()).isEmpty();
    assertThat(spy(StreamCloseFrame.class).errorMessage()).isEmpty();
  }
}
