package org.interledger.stream.frames;

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

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Error codes are sent in StreamClose and ConnectionClose frames to indicate what caused the stream or connection to be
 * closed.
 */
public enum ErrorCodes implements ErrorCode {

  /**
   * The application using STREAM closed the stream or connection with an error.
   */
  UnsupportedError(UNSUPPORTED_ERROR, "An unsupported error code was supplied."),

  /**
   * Indicates the stream or connection closed normally.
   */
  NoError(NO_ERROR, "The stream or connection closed normally."),

  /**
   * The endpoint encountered an unexpected error.
   */
  InternalError(INTERNAL_ERROR, "The endpoint encountered an unexpected error."),

  /**
   * The endpoint is temporarily overloaded and unable to process the packet.
   */
  EndpointBusy(ENDPOINT_BUSY, "The endpoint is temporarily overloaded and unable to process the packet."),

  /**
   * The other endpoint exceeded the flow control limits advertised.
   */
  FlowControlError(FLOW_CONTROL_ERROR, "The other endpoint exceeded the flow control limits advertised."),

  /**
   * The other endpoint opened more streams than allowed.
   */
  StreamIdError(STREAM_ID_ERROR, "The other endpoint opened more streams than allowed."),

  /**
   * The other endpoint sent frames for a stream that is already closed.
   */
  StreamStateError(STREAM_STATE_ERROR, "The other endpoint sent frames for a stream that is already closed."),

  /**
   * The other endpoint sent a frame with invalid syntax.
   */
  FrameFormatError(FRAME_FORMAT_ERROR, "The other endpoint sent a frame with invalid syntax."),

  /**
   * The other endpoint sent invalid data or otherwise violated the protocol.
   */
  ProtocolViolation(PROTOCOL_VIOLATION, "The other endpoint sent invalid data or otherwise violated the protocol."),

  /**
   * The application using STREAM closed the stream or connection with an error.
   */
  ApplicationError(APPLICATION_ERROR, "The application using STREAM closed the stream or connection with an error.");

  private final short code;
  private final String description;

  ErrorCodes(final short code, final String description) {
    this.code = code;
    this.description = Objects.requireNonNull(description);
  }

  /**
   * Helper method to construct an instance of {@link ErrorCode}.
   *
   * @param code The definitive identifier of the error.
   *
   * @return An {@link ErrorCode}
   */
  public static ErrorCode of(final short code) {
    switch (code) {
      case NO_ERROR: {
        return ErrorCode.builder().from(NoError).build();
      }
      case INTERNAL_ERROR: {
        return ErrorCode.builder().from(InternalError).build();
      }
      case ENDPOINT_BUSY: {
        return ErrorCode.builder().from(EndpointBusy).build();
      }
      case FLOW_CONTROL_ERROR: {
        return ErrorCode.builder().from(FlowControlError).build();
      }
      case STREAM_ID_ERROR: {
        return ErrorCode.builder().from(StreamIdError).build();
      }
      case STREAM_STATE_ERROR: {
        return ErrorCode.builder().from(StreamStateError).build();
      }
      case FRAME_FORMAT_ERROR: {
        return ErrorCode.builder().from(FrameFormatError).build();
      }
      case PROTOCOL_VIOLATION: {
        return ErrorCode.builder().from(ProtocolViolation).build();
      }
      case APPLICATION_ERROR: {
        return ErrorCode.builder().from(ApplicationError).build();
      }
      default: {
        return ErrorCode.builder()
            .from(UnsupportedError)
            .code(code)
            .build();
      }
    }
  }

  @Override
  public short code() {
    return code;
  }

  @Override
  public String description() {
    return description;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", ErrorCodes.class.getSimpleName() + "[", "]")
        .add("code=" + code)
        .add("description='" + description + "'")
        .toString();
  }
}
