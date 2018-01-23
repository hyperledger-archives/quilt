package org.interledger.codecs.ilp.asn;

import static java.lang.String.format;

import org.interledger.InterledgerPacket;
import org.interledger.codecs.asn.AsnSequence;
import org.interledger.codecs.framework.CodecException;
import org.interledger.ilp.InterledgerFulfillPacket;
import org.interledger.ilp.InterledgerPreparePacket;
import org.interledger.ilp.InterledgerRejectPacket;

/**
 * Generic Interledger packet wrapper for reading packets where the type is not known
 */
public class AsnInterledgerPacket extends AsnSequence<InterledgerPacket> {

  @Override
  protected InterledgerPacket decode() {
    return getValueAt(1);
  }

  @Override
  protected void encode(InterledgerPacket value) {

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

  @Override
  protected void onSetValue(int index) {

    if (index == 0) {

      int typeCode = getValueAt(0);

      //The packet type has been set so set the packet data
      switch (typeCode) {
        case InterledgerPacketTypes.PREPARE:
          setElementAt(1, new AsnInterledgerPreparePacket());
          return;
        case InterledgerPacketTypes.FULFILL:
          setElementAt(1, new AsnInterledgerFulfillPacket());
          return;
        case InterledgerPacketTypes.REJECT:
          setElementAt(1, new AsnInterledgerRejectPacket());
          return;
      }

      throw new CodecException(
          format("Unknown Interledger packet type code: %s", typeCode));
    }
  }

  private static class InterledgerPacketTypes {

    static final int PREPARE = 12;
    static final int FULFILL = 13;
    static final int REJECT = 14;

  }

}
