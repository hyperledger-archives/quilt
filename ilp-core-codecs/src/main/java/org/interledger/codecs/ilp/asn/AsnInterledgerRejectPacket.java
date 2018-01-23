package org.interledger.codecs.ilp.asn;

import org.interledger.codecs.asn.AsnOctetString;
import org.interledger.codecs.asn.AsnSequence;
import org.interledger.codecs.asn.AsnSizeConstraint;
import org.interledger.codecs.asn.AsnUTF8String;
import org.interledger.ilp.InterledgerRejectPacket;

public class AsnInterledgerRejectPacket extends AsnSequence<InterledgerRejectPacket> {

  public AsnInterledgerRejectPacket() {
    super(
        new AsnInterledgerErrorCode(),
        new AsnInterledgerAddress(),
        new AsnUTF8String(new AsnSizeConstraint(0, 8191)),
        new AsnOctetString(new AsnSizeConstraint(0, 32767))
    );
  }

  @Override
  protected InterledgerRejectPacket decode() {
    return InterledgerRejectPacket.builder()
        .code(getValueAt(0))
        .triggeredBy(getValueAt(1))
        .message(getValueAt(2))
        .data(getValueAt(3))
        .build();
  }

  @Override
  protected void encode(InterledgerRejectPacket value) {
    setValueAt(0, value.getCode());
    setValueAt(1, value.getTriggeredBy());
    setValueAt(2, value.getMessage());
    setValueAt(3, value.getData());
  }

}
