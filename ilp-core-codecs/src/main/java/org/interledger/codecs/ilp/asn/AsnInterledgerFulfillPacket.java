package org.interledger.codecs.ilp.asn;

import org.interledger.codecs.asn.AsnOctetString;
import org.interledger.codecs.asn.AsnSequence;
import org.interledger.codecs.asn.AsnSizeConstraint;
import org.interledger.ilp.InterledgerFulfillPacket;

public class AsnInterledgerFulfillPacket extends AsnSequence<InterledgerFulfillPacket> {

  public AsnInterledgerFulfillPacket() {
    super(
        new AsnFulfillment(),
        new AsnOctetString(new AsnSizeConstraint(0, 32767)));
  }

  @Override
  protected InterledgerFulfillPacket decode() {
    return InterledgerFulfillPacket.builder()
        .fulfillment(getValueAt(0))
        .data(getValueAt(1))
        .build();
  }

  @Override
  protected void encode(InterledgerFulfillPacket value) {
    setValueAt(0, value.getFulfillment());
    setValueAt(1, value.getData());
  }

}
