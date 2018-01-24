package org.interledger.codecs.ilp.asn;

import org.interledger.codecs.asn.AsnOctetString;
import org.interledger.codecs.asn.AsnSequence;
import org.interledger.codecs.asn.AsnSizeConstraint;
import org.interledger.codecs.asn.AsnUint64;
import org.interledger.ilp.InterledgerPreparePacket;

public class AsnInterledgerPreparePacketData extends AsnSequence<InterledgerPreparePacket> {

  public AsnInterledgerPreparePacketData() {
    super(
        new AsnUint64(),
        new AsnTimestamp(),
        new AsnCondition(),
        new AsnInterledgerAddress(),
        new AsnOctetString(new AsnSizeConstraint(0, 32768))  //TODO Implement max getLength
    );
  }

  @Override
  protected InterledgerPreparePacket decode() {
    return InterledgerPreparePacket.builder()
        .amount(getValueAt(0))
        .expiresAt(getValueAt(1))
        .executionCondition(getValueAt(2))
        .destination(getValueAt(3))
        .data(getValueAt(4))
        .build();
  }

  @Override
  protected void encode(InterledgerPreparePacket value) {
    setValueAt(0, value.getAmount());
    setValueAt(1, value.getExpiresAt());
    setValueAt(2, value.getExecutionCondition());
    setValueAt(3, value.getDestination());
    setValueAt(4, value.getData());
  }

}
