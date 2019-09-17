package org.interledger.stream.frames;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.stream.frames.ErrorCodeConstants.*;

public class ErrorCodeTest {

  private final List<Short> expected = Lists.newArrayList(
    NO_ERROR,
    INTERNAL_ERROR,
    ENDPOINT_BUSY,
    FLOW_CONTROL_ERROR,
    STREAM_ID_ERROR,
    STREAM_STATE_ERROR,
    FRAME_FORMAT_ERROR,
    PROTOCOL_VIOLATION,
    APPLICATION_ERROR
  );

  @Test
  public void of() {
    assertThat(expected.stream().map(ErrorCode::of).collect(Collectors.toList()))
        .isEqualTo(Arrays.asList(ErrorCode.values()));
  }

  @Test
  public void description() {
    assertThat(ErrorCode.NoError.getDescription()).isEqualTo("The stream or connection closed normally.");
    assertThat(ErrorCode.InternalError.getDescription()).isEqualTo("The endpoint encountered an unexpected error.");
    assertThat(ErrorCode.EndpointBusy.getDescription()).isEqualTo("The endpoint is temporarily overloaded and unable to process the packet.");
    assertThat(ErrorCode.FlowControlError.getDescription()).isEqualTo("The other endpoint exceeded the flow control limits advertised.");
    assertThat(ErrorCode.StreamIdError.getDescription()).isEqualTo("The other endpoint opened more streams than allowed.");
    assertThat(ErrorCode.StreamStateError.getDescription()).isEqualTo("The other endpoint sent frames for a stream that is already closed.");
    assertThat(ErrorCode.FrameFormatError.getDescription()).isEqualTo("The other endpoint sent a frame with invalid syntax.");
    assertThat(ErrorCode.ProtocolViolation.getDescription()).isEqualTo("The other endpoint sent invalid data or otherwise violated the protocol.");
    assertThat(ErrorCode.ApplicationError.getDescription()).isEqualTo("The application using STREAM closed the stream or connection with an error.");
  }
}
