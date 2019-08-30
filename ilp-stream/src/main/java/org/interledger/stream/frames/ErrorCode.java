package org.interledger.stream.frames;

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
  NoError((short) 1, "The stream or connection closed normally."),

  /**
   * The endpoint encountered an unexpected error.
   */
  InternalError((short) 2, "The endpoint encountered an unexpected error."),

  /**
   * The endpoint is temporarily overloaded and unable to process the packet.
   */
  EndpointBusy((short) 3, "The endpoint is temporarily overloaded and unable to process the packet."),

  /**
   * The other endpoint exceeded the flow control limits advertised.
   */
  FlowControlError((short) 4, "The other endpoint exceeded the flow control limits advertised."),

  /**
   * The other endpoint opened more streams than allowed.
   */
  StreamIdError((short) 5, "The other endpoint opened more streams than allowed."),

  /**
   * The other endpoint sent frames for a stream that is already closed.
   */
  StreamStateError((short) 6, "The other endpoint sent frames for a stream that is already closed."),

  /**
   * The other endpoint sent a frame with invalid syntax.
   */
  FrameFormatError((short) 7, "The other endpoint sent a frame with invalid syntax."),

  /**
   * The other endpoint sent invalid data or otherwise violated the protocol.
   */
  ProtocolViolation((short) 8, "The other endpoint sent invalid data or otherwise violated the protocol."),

  /**
   * The application using STREAM closed the stream or connection with an error.
   */
  ApplicationError((short) 9, "The application using STREAM closed the stream or connection with an error.");

  private final short code;
  private final String description;

  ErrorCode(short code, String description) {
    this.code = Objects.requireNonNull(code);
    this.description = Objects.requireNonNull(description);
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
