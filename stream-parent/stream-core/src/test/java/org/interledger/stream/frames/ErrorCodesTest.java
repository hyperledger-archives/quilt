package org.interledger.stream.frames;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.stream.frames.ErrorCodeConstants.APPLICATION_ERROR;
import static org.interledger.stream.frames.ErrorCodeConstants.ENDPOINT_BUSY;
import static org.interledger.stream.frames.ErrorCodeConstants.FLOW_CONTROL_ERROR;
import static org.interledger.stream.frames.ErrorCodeConstants.FRAME_FORMAT_ERROR;
import static org.interledger.stream.frames.ErrorCodeConstants.INTERNAL_ERROR;
import static org.interledger.stream.frames.ErrorCodeConstants.NO_ERROR;
import static org.interledger.stream.frames.ErrorCodeConstants.PROTOCOL_VIOLATION;
import static org.interledger.stream.frames.ErrorCodeConstants.STREAM_ID_ERROR;
import static org.interledger.stream.frames.ErrorCodeConstants.STREAM_STATE_ERROR;
import static org.interledger.stream.frames.ErrorCodeConstants.UNSUPPORTED_ERROR;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ErrorCodesTest {

  private final List<Short> expected = Lists.newArrayList(
      UNSUPPORTED_ERROR,
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
    assertThat(expected.stream().map(ErrorCodes::of).collect(Collectors.toList()))
        .isEqualTo(Arrays.asList(ErrorCodes.values()).stream()
            .map(ErrorCode.builder()::from)
            .map(ErrorCodeBuilder::build)
            .collect(Collectors.toList())
      );
  }

  @Test
  public void codeAnddescription() {
    final String unknownErrorDesc = "An unsupported error code was supplied.";
    final String noErrorDesc = "The stream or connection closed normally.";
    final String internalErrorDesc = "The endpoint encountered an unexpected error.";
    final String endpointBusyDesc = "The endpoint is temporarily overloaded and unable to process the packet.";
    final String flowControlErrorDesc = "The other endpoint exceeded the flow control limits advertised.";
    final String streamIdErrorDesc = "The other endpoint opened more streams than allowed.";
    final String streamStateErrorDesc = "The other endpoint sent frames for a stream that is already closed.";
    final String frameFormatErrorDesc = "The other endpoint sent a frame with invalid syntax.";
    final String protocolViolationDesc = "The other endpoint sent invalid data or otherwise violated the protocol.";
    final String applicationErrorDesc = "The application using STREAM closed the stream or connection with an error.";

    assertThat(ErrorCodes.UnsupportedError.description()).isEqualTo(unknownErrorDesc);
    assertThat(ErrorCodes.UnsupportedError.code()).isEqualTo((short) 0);
    assertThat(ErrorCodes.NoError.code()).isEqualTo((short) 0x01);
    assertThat(ErrorCodes.NoError.description()).isEqualTo(noErrorDesc);
    assertThat(ErrorCodes.InternalError.code()).isEqualTo((short) 0x02);
    assertThat(ErrorCodes.InternalError.description()).isEqualTo(internalErrorDesc);
    assertThat(ErrorCodes.EndpointBusy.code()).isEqualTo((short) 0x03);
    assertThat(ErrorCodes.EndpointBusy.description()).isEqualTo(endpointBusyDesc);
    assertThat(ErrorCodes.FlowControlError.code()).isEqualTo((short) 0x04);
    assertThat(ErrorCodes.FlowControlError.description()).isEqualTo(flowControlErrorDesc);
    assertThat(ErrorCodes.StreamIdError.code()).isEqualTo((short) 0x05);
    assertThat(ErrorCodes.StreamIdError.description()).isEqualTo(streamIdErrorDesc);
    assertThat(ErrorCodes.StreamStateError.code()).isEqualTo((short) 0x06);
    assertThat(ErrorCodes.StreamStateError.description()).isEqualTo(streamStateErrorDesc);
    assertThat(ErrorCodes.FrameFormatError.code()).isEqualTo((short) 0x07);
    assertThat(ErrorCodes.FrameFormatError.description()).isEqualTo(frameFormatErrorDesc);
    assertThat(ErrorCodes.ProtocolViolation.code()).isEqualTo((short) 0x08);
    assertThat(ErrorCodes.ProtocolViolation.description()).isEqualTo(protocolViolationDesc);
    assertThat(ErrorCodes.ApplicationError.code()).isEqualTo((short) 0x09);
    assertThat(ErrorCodes.ApplicationError.description()).isEqualTo(applicationErrorDesc);
  }

  @Test
  public void testToStringFormatting() {
    String errorCodeFormatting = "ErrorCodes[code=1, description='The stream or connection closed normally.']";
    assertThat(ErrorCodes.NoError.toString()).isEqualTo(errorCodeFormatting);
  }

  @Test
  public void testUnsupportedErrorCodes() {
    for (short i = 10; i <= 255; i++) {
      assertThat(ErrorCodes.of(i).code()).isEqualTo(i);
      assertThat(ErrorCodes.of(i).description()).isEqualTo(ErrorCodes.UnsupportedError.description());
    }
  }
}
