package org.interledger.stream.frames;

public interface ErrorCodeConstants {

  short NO_ERROR = (short) 0x01;
  short INTERNAL_ERROR = (short) 0x02;
  short ENDPOINT_BUSY = (short) 0x03;
  short FLOW_CONTROL_ERROR = (short) 0x04;
  short STREAM_ID_ERROR = (short) 0x05;
  short STREAM_STATE_ERROR = (short) 0x06;
  short FRAME_FORMAT_ERROR = (short) 0x07;
  short PROTOCOL_VIOLATION = (short) 0x08;
  short APPLICATION_ERROR = (short) 0x09;

}
