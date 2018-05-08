package org.interledger.core.asn.codecs;

import static java.lang.String.format;

import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.encoding.asn.codecs.AsnOpenTypeCodec;
import org.interledger.encoding.asn.codecs.AsnSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnUint8Codec;
import org.interledger.encoding.asn.framework.CodecException;

/**
 * ASN.1 object representing an {@link InterledgerPacket} including it's type code prefix.
 */
public class AsnInterledgerPacketCodec<T extends InterledgerPacket> extends AsnSequenceCodec<T> {


  /**
   * Default constructor.
   */
  public AsnInterledgerPacketCodec() {
    super(new AsnUint8Codec(), null);
    AsnUint8Codec asnTypeId = (AsnUint8Codec) getCodecAt(0);
    asnTypeId.setValueChangedEventListener((codec) -> {
      onTypeIdChanged(codec.decode());
    });
  }

  @Override
  public T decode() {
    return getValueAt(1);
  }

  @Override
  public void encode(T value) {

    if (value instanceof InterledgerPreparePacket) {
      setValueAt(0, InterledgerPacketTypes.PREPARE);
    } else if (value instanceof InterledgerFulfillPacket) {
      setValueAt(0, InterledgerPacketTypes.FULFILL);
    } else if (value instanceof InterledgerRejectPacket) {
      setValueAt(0, InterledgerPacketTypes.REJECT);
    } else {
      throw new CodecException(
          format("Unknown Interledger Packet Type: %s", value.getClass().getName()));
    }

    setValueAt(1, value);
  }

  protected void onTypeIdChanged(int typeId) {

    //The packet type has been set so set the packet data
    switch (typeId) {
      case InterledgerPacketTypes.PREPARE:
        setCodecAt(1, new AsnOpenTypeCodec<>(new AsnInterledgerPreparePacketDataCodec()));
        return;
      case InterledgerPacketTypes.FULFILL:
        setCodecAt(1, new AsnOpenTypeCodec<>(new AsnInterledgerFulfillPacketDataCodec()));
        return;
      case InterledgerPacketTypes.REJECT:
        setCodecAt(1, new AsnOpenTypeCodec<>(new AsnInterledgerRejectPacketDataCodec()));
        return;
      default:
        throw new CodecException(
            format("Unknown Interledger packet type code: %s", typeId));
    }

  }

  private static class InterledgerPacketTypes {

    static final int PREPARE = 12;
    static final int FULFILL = 13;
    static final int REJECT = 14;

  }

}
