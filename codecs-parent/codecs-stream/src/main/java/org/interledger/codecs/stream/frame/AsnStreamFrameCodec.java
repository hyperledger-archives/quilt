package org.interledger.codecs.stream.frame;

import static org.interledger.stream.frames.StreamFrameConstants.CONNECTION_ASSET_DETAILS;
import static org.interledger.stream.frames.StreamFrameConstants.CONNECTION_CLOSE;
import static org.interledger.stream.frames.StreamFrameConstants.CONNECTION_DATA_BLOCKED;
import static org.interledger.stream.frames.StreamFrameConstants.CONNECTION_DATA_MAX;
import static org.interledger.stream.frames.StreamFrameConstants.CONNECTION_MAX_STREAM_ID;
import static org.interledger.stream.frames.StreamFrameConstants.CONNECTION_NEW_ADDRESS;
import static org.interledger.stream.frames.StreamFrameConstants.CONNECTION_STREAM_ID_BLOCKED;
import static org.interledger.stream.frames.StreamFrameConstants.STREAM_CLOSE;
import static org.interledger.stream.frames.StreamFrameConstants.STREAM_DATA;
import static org.interledger.stream.frames.StreamFrameConstants.STREAM_DATA_BLOCKED;
import static org.interledger.stream.frames.StreamFrameConstants.STREAM_DATA_MAX;
import static org.interledger.stream.frames.StreamFrameConstants.STREAM_MONEY;
import static org.interledger.stream.frames.StreamFrameConstants.STREAM_MONEY_BLOCKED;
import static org.interledger.stream.frames.StreamFrameConstants.STREAM_MONEY_MAX;

import org.interledger.encoding.asn.codecs.AsnOpenTypeCodec;
import org.interledger.encoding.asn.codecs.AsnSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnUint8Codec;
import org.interledger.encoding.asn.framework.CodecException;
import org.interledger.stream.frames.StreamFrame;

public class AsnStreamFrameCodec<T extends StreamFrame> extends AsnSequenceCodec<T> {

  /**
   * Default constructor.
   */
  public AsnStreamFrameCodec() {
    super(
        new AsnUint8Codec(), // type
        null // FrameCodec
    );
    AsnUint8Codec frameTypeCodec = (AsnUint8Codec) getCodecAt(0);
    frameTypeCodec.setValueChangedEventListener((codec) -> {
      onFrameTypeChanged(codec.decode());
    });
  }

  /**
   * Decode and return the value read into the codec during serialization.
   *
   * @return the decoded object
   */
  @Override
  public T decode() {
    return getValueAt(1);
  }

  /**
   * Encode the provided value into the codec to be written during serialization.
   *
   * @param value the value to encode
   */
  @Override
  public void encode(T value) {
    setValueAt(0, value.streamFrameType().code());
    setValueAt(1, value);
  }

  protected void onFrameTypeChanged(short streamFrameTypeCode) {
    //The frame type has been set so set the packet data
    switch (streamFrameTypeCode) {
      case CONNECTION_CLOSE: {
        setCodecAt(1, new AsnOpenTypeCodec<>(new AsnConnectionCloseFrameDataCodec()));
        return;
      }
      case CONNECTION_NEW_ADDRESS: {
        setCodecAt(1, new AsnOpenTypeCodec<>(new AsnConnectionNewAddressFrameDataCodec()));
        return;
      }
      case CONNECTION_DATA_MAX: {
        setCodecAt(1, new AsnOpenTypeCodec<>(new AsnConnectionDataMaxFrameDataCodec()));
        return;
      }
      case CONNECTION_DATA_BLOCKED: {
        setCodecAt(1, new AsnOpenTypeCodec<>(new AsnConnectionDataBlockedFrameDataCodec()));
        return;
      }
      case CONNECTION_MAX_STREAM_ID: {
        setCodecAt(1, new AsnOpenTypeCodec<>(new AsnConnectionMaxStreamIdFrameDataCodec()));
        return;
      }
      case CONNECTION_STREAM_ID_BLOCKED: {
        setCodecAt(1, new AsnOpenTypeCodec<>(new AsnConnectionStreamIdBlockedFrameDataCodec()));
        return;
      }
      case CONNECTION_ASSET_DETAILS: {
        setCodecAt(1, new AsnOpenTypeCodec<>(new AsnConnectionAssetDetailsFrameDataCodec()));
        return;
      }
      case STREAM_CLOSE: {
        setCodecAt(1, new AsnOpenTypeCodec<>(new AsnStreamCloseFrameDataCodec()));
        return;
      }
      case STREAM_MONEY: {
        setCodecAt(1, new AsnOpenTypeCodec<>(new AsnStreamMoneyFrameDataCodec()));
        return;
      }
      case STREAM_MONEY_MAX: {
        setCodecAt(1, new AsnOpenTypeCodec<>(new AsnStreamMoneyMaxFrameDataCodec()));
        return;
      }
      case STREAM_MONEY_BLOCKED: {
        setCodecAt(1, new AsnOpenTypeCodec<>(new AsnStreamMoneyBlockedFrameDataCodec()));
        return;
      }
      case STREAM_DATA: {
        setCodecAt(1, new AsnOpenTypeCodec<>(new AsnStreamDataFrameDataCodec()));
        return;
      }
      case STREAM_DATA_MAX: {
        setCodecAt(1, new AsnOpenTypeCodec<>(new AsnStreamDataMaxFrameDataCodec()));
        return;
      }
      case STREAM_DATA_BLOCKED: {
        setCodecAt(1, new AsnOpenTypeCodec<>(new AsnStreamDataBlockedFrameDataCodec()));
        return;
      }
      default: {
        throw new CodecException(
            String.format("Unknown STREAM Frame packet type: %s", streamFrameTypeCode));
      }
    }

  }

}
