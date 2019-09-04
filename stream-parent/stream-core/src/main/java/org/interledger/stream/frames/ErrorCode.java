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

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Error codes are sent in StreamClose and ConnectionClose frames to indicate what caused the stream or connection to be
 * closed.
 */
public enum ErrorCode {

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

  ErrorCode(short code, String description) {
    this.code = Objects.requireNonNull(code);
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
        return NoError;
      }
      case INTERNAL_ERROR: {
        return InternalError;
      }
      case ENDPOINT_BUSY: {
        return EndpointBusy;
      }
      case FLOW_CONTROL_ERROR: {
        return FlowControlError;
      }
      case STREAM_ID_ERROR: {
        return StreamIdError;
      }
      case STREAM_STATE_ERROR: {
        return StreamStateError;
      }
      case FRAME_FORMAT_ERROR: {
        return FrameFormatError;
      }
      case PROTOCOL_VIOLATION: {
        return ProtocolViolation;
      }
      case APPLICATION_ERROR: {
        return ApplicationError;
      }
      default: {
        throw new RuntimeException("Invalid Stream Error code: " + code);
      }
    }
  }

  public short getCode() {
    return code;
  }

  public String getDescription() {
    return description;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", ErrorCode.class.getSimpleName() + "[", "]")
        .add("code=" + code)
        .add("description='" + description + "'")
        .toString();
  }
}
