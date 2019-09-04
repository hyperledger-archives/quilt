package org.interledger.stream.frames;

public interface StreamFrameConstants {

  short CONNECTION_CLOSE = (short) 1;
  short CONNECTION_NEW_ADDRESS = (short) 2;
  short CONNECTION_DATA_MAX = (short) 3;
  short CONNECTION_DATA_BLOCKED = (short) 4;
  short CONNECTION_MAX_STREAM_ID = (short) 5;
  short CONNECTION_STREAM_ID_BLOCKED = (short) 6;
  short CONNECTION_ASSET_DETAILS = (short) 7;

  short STREAM_CLOSE = (short) 10;
  short STREAM_MONEY = (short) 11;
  short STREAM_MONEY_MAX = (short) 12;
  short STREAM_MONEY_BLOCKED = (short) 13;
  short STREAM_DATA = (short) 14;
  short STREAM_DATA_MAX = (short) 15;
  short STREAM_DATA_BLOCKED = (short) 16;

}
