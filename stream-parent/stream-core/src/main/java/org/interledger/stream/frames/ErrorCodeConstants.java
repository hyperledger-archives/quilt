package org.interledger.stream.frames;

public interface ErrorCodeConstants {

  short NO_ERROR = (short) 1;
  short INTERNAL_ERROR = (short) 2;
  short ENDPOINT_BUSY = (short) 3;
  short FLOW_CONTROL_ERROR = (short) 4;
  short STREAM_ID_ERROR = (short) 5;
  short STREAM_STATE_ERROR = (short) 6;
  short FRAME_FORMAT_ERROR = (short) 7;
  short PROTOCOL_VIOLATION = (short) 8;
  short APPLICATION_ERROR = (short) 9;

}
