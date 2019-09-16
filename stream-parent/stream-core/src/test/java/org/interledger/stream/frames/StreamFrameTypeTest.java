package org.interledger.stream.frames;

import com.google.common.collect.Lists;
import org.junit.Test;

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

  @Test
  public void fromCode() {
    assertThat(expected.stream().map(StreamFrameType::fromCode).collect(Collectors.toList()))
        .isEqualTo(Arrays.asList(StreamFrameType.values()));
  }
}
