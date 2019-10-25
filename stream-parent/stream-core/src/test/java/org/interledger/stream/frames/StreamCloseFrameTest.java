package org.interledger.stream.frames;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

public class StreamCloseFrameTest {

  @Test
  public void emptyMessage() {
    StreamCloseFrame frame = StreamCloseFrame.builder()
        .streamId(UnsignedLong.ZERO)
        .errorCode(ErrorCodes.NoError)
        .build();
    assertThat(frame.errorMessage()).isEmpty();

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
    assertThat(interfaceFrame.errorMessage()).isEmpty();
  }
}
