package org.interledger.stream.frames;

public interface StreamFrameConstants {

  short CONNECTION_CLOSE = (short) 0x01;
  short CONNECTION_NEW_ADDRESS = (short) 0x02;
  short CONNECTION_DATA_MAX = (short) 0x03;
  short CONNECTION_DATA_BLOCKED = (short) 0x04;
  short CONNECTION_MAX_STREAM_ID = (short) 0x05;
  short CONNECTION_STREAM_ID_BLOCKED = (short) 0x06;
  short CONNECTION_ASSET_DETAILS = (short) 0x07;

  short STREAM_CLOSE = (short) 0x10;
  short STREAM_MONEY = (short) 0x11;
  short STREAM_MONEY_MAX = (short) 0x12;
  short STREAM_MONEY_BLOCKED = (short) 0x13;
  short STREAM_DATA = (short) 0x14;
  short STREAM_DATA_MAX = (short) 0x15;
  short STREAM_DATA_BLOCKED = (short) 0x16;

}
