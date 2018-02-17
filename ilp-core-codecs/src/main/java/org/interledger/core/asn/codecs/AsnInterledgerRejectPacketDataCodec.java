package org.interledger.core.asn.codecs;

import org.interledger.core.InterledgerRejectPacket;
import org.interledger.encoding.asn.codecs.AsnOctetStringCodec;
import org.interledger.encoding.asn.codecs.AsnSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;
import org.interledger.encoding.asn.codecs.AsnUtf8StringCodec;

public class AsnInterledgerRejectPacketDataCodec extends AsnSequenceCodec<InterledgerRejectPacket> {

  /**
   * Default constructor.
   */
  public AsnInterledgerRejectPacketDataCodec() {
    super(
        new AsnInterledgerErrorCodeCodec(),
        new AsnInterledgerAddressCodec(),
        new AsnUtf8StringCodec(new AsnSizeConstraint(0, 8192)),
        new AsnOctetStringCodec(new AsnSizeConstraint(0, 32768))
    );
  }

  @Override
  public InterledgerRejectPacket decode() {
    return InterledgerRejectPacket.builder()
        .code(getValueAt(0))
        .triggeredBy(getValueAt(1))
        .message(getValueAt(2))
        .data(getValueAt(3))
        .build();
  }

  @Override
  public void encode(InterledgerRejectPacket value) {
    setValueAt(0, value.getCode());
    setValueAt(1, value.getTriggeredBy());
    setValueAt(2, value.getMessage());
    setValueAt(3, value.getData());
  }

}
