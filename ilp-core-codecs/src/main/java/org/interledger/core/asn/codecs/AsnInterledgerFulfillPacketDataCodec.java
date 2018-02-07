package org.interledger.core.asn.codecs;

import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.encoding.asn.codecs.AsnOctetStringCodec;
import org.interledger.encoding.asn.codecs.AsnSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;

public class AsnInterledgerFulfillPacketDataCodec
    extends AsnSequenceCodec<InterledgerFulfillPacket> {

  /**
   * Default constructor.
   */
  public AsnInterledgerFulfillPacketDataCodec() {
    super(
        new AsnFulfillmentCodec(),
        new AsnOctetStringCodec(new AsnSizeConstraint(0, 32768)));
  }

  @Override
  public InterledgerFulfillPacket decode() {
    return InterledgerFulfillPacket.builder()
        .fulfillment(getValueAt(0))
        .data(getValueAt(1))
        .build();
  }

  @Override
  public void encode(InterledgerFulfillPacket value) {
    setValueAt(0, value.getFulfillment());
    setValueAt(1, value.getData());
  }

}
