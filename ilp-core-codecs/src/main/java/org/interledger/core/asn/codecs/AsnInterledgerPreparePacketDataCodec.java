package org.interledger.core.asn.codecs;

import org.interledger.core.InterledgerPreparePacket;
import org.interledger.encoding.asn.codecs.AsnOctetStringCodec;
import org.interledger.encoding.asn.codecs.AsnSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;
import org.interledger.encoding.asn.codecs.AsnUint64Codec;

public class AsnInterledgerPreparePacketDataCodec
    extends AsnSequenceCodec<InterledgerPreparePacket> {

  /**
   * Default constructor.
   */
  public AsnInterledgerPreparePacketDataCodec() {
    super(
        new AsnUint64Codec(),
        new AsnTimestampCodec(),
        new AsnConditionCodec(),
        new AsnInterledgerAddressCodec(),
        new AsnOctetStringCodec(new AsnSizeConstraint(0, 32768))  //TODO Implement max getLength
    );
  }

  @Override
  public InterledgerPreparePacket decode() {
    return InterledgerPreparePacket.builder()
        .amount(getValueAt(0))
        .expiresAt(getValueAt(1))
        .executionCondition(getValueAt(2))
        .destination(getValueAt(3))
        .data(getValueAt(4))
        .build();
  }

  @Override
  public void encode(InterledgerPreparePacket value) {
    setValueAt(0, value.getAmount());
    setValueAt(1, value.getExpiresAt());
    setValueAt(2, value.getExecutionCondition());
    setValueAt(3, value.getDestination());
    setValueAt(4, value.getData());
  }

}
