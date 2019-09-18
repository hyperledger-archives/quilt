package org.interledger.stream.frames;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StreamFrameTest {

  @Test
  public void empty() {
    assertThat(StreamFrame.EMPTY_DATA).isEqualTo(new byte[0]);
  }
}
