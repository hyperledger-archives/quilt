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

import com.google.common.collect.Lists;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void of() {
    assertThat(expected.stream().map(ErrorCode::of).collect(Collectors.toList()))
        .isEqualTo(Arrays.asList(ErrorCode.values()));
  }

  @Test
  public void ofThrows() {
    expectedException.expect(RuntimeException.class);
    ErrorCode.of((short) 0x17);
  }

  @Test
  public void codeAndDescription() {
    final String noErrorDesc = "The stream or connection closed normally.";
    final String internalErrorDesc = "The endpoint encountered an unexpected error.";
    final String endpointBusyDesc = "The endpoint is temporarily overloaded and unable to process the packet.";
    final String flowControlErrorDesc = "The other endpoint exceeded the flow control limits advertised.";
    final String streamIdErrorDesc = "The other endpoint opened more streams than allowed.";
    final String streamStateErrorDesc = "The other endpoint sent frames for a stream that is already closed.";
    final String frameFormatErrorDesc = "The other endpoint sent a frame with invalid syntax.";
    final String protocolViolationDesc = "The other endpoint sent invalid data or otherwise violated the protocol.";
    final String applicationErrorDesc = "The application using STREAM closed the stream or connection with an error.";

    assertThat(ErrorCode.NoError.getCode()).isEqualTo((short) 0x01);
    assertThat(ErrorCode.NoError.getDescription()).isEqualTo(noErrorDesc);
    assertThat(ErrorCode.InternalError.getCode()).isEqualTo((short) 0x02);
    assertThat(ErrorCode.InternalError.getDescription()).isEqualTo(internalErrorDesc);
    assertThat(ErrorCode.EndpointBusy.getCode()).isEqualTo((short) 0x03);
    assertThat(ErrorCode.EndpointBusy.getDescription()).isEqualTo(endpointBusyDesc);
    assertThat(ErrorCode.FlowControlError.getCode()).isEqualTo((short) 0x04);
    assertThat(ErrorCode.FlowControlError.getDescription()).isEqualTo(flowControlErrorDesc);
    assertThat(ErrorCode.StreamIdError.getCode()).isEqualTo((short) 0x05);
    assertThat(ErrorCode.StreamIdError.getDescription()).isEqualTo(streamIdErrorDesc);
    assertThat(ErrorCode.StreamStateError.getCode()).isEqualTo((short) 0x06);
    assertThat(ErrorCode.StreamStateError.getDescription()).isEqualTo(streamStateErrorDesc);
    assertThat(ErrorCode.FrameFormatError.getCode()).isEqualTo((short) 0x07);
    assertThat(ErrorCode.FrameFormatError.getDescription()).isEqualTo(frameFormatErrorDesc);
    assertThat(ErrorCode.ProtocolViolation.getCode()).isEqualTo((short) 0x08);
    assertThat(ErrorCode.ProtocolViolation.getDescription()).isEqualTo(protocolViolationDesc);
    assertThat(ErrorCode.ApplicationError.getCode()).isEqualTo((short) 0x09);
    assertThat(ErrorCode.ApplicationError.getDescription()).isEqualTo(applicationErrorDesc);
  }

  @Test
  public void testToStringFormatting() {
    String errorCodeFormatting = "ErrorCode[code=1, description='The stream or connection closed normally.']";
    assertThat(ErrorCode.NoError.toString()).isEqualTo(errorCodeFormatting);
  }

}
