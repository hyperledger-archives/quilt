package org.interledger.codecs.ilp.asn;

import static java.lang.String.format;

import org.interledger.InterledgerPacket;
import org.interledger.codecs.asn.AsnSequence;
import org.interledger.codecs.asn.AsnUint8;
import org.interledger.codecs.framework.CodecException;
import org.interledger.ilp.InterledgerFulfillPacket;
import org.interledger.ilp.InterledgerPreparePacket;
import org.interledger.ilp.InterledgerRejectPacket;

/**
 * Generic Interledger packet wrapper for reading packets where the type is not known
 */
public class AsnInterledgerPacket<T extends InterledgerPacket> extends AsnSequence<T> {


  public AsnInterledgerPacket() {
    super(new AsnUint8(), null);
    AsnUint8 asnTypeId = (AsnUint8) getElementAt(0);
    asnTypeId.setValueChangeListener(this::onTypeIdChanged);
  }

  @Override
  protected T decode() {
    return getValueAt(1);
  }

  @Override
  protected void encode(T value) {

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

  protected void onTypeIdChanged(Integer typeId) {

    //The packet type has been set so set the packet data
    switch (typeId) {
      case InterledgerPacketTypes.PREPARE:
        setElementAt(1, new AsnInterledgerPreparePacketData());
        return;
      case InterledgerPacketTypes.FULFILL:
        setElementAt(1, new AsnInterledgerFulfillPacketData());
        return;
      case InterledgerPacketTypes.REJECT:
        setElementAt(1, new AsnInterledgerRejectPacketData());
        return;
    }

    throw new CodecException(
        format("Unknown Interledger packet type code: %s", typeId));
  }

  private static class InterledgerPacketTypes {

    static final int PREPARE = 12;
    static final int FULFILL = 13;
    static final int REJECT = 14;

  }

}
