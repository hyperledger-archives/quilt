package org.interledger.btp.asn.codecs;

import org.interledger.btp.BtpSubProtocol;
import org.interledger.encoding.asn.codecs.AsnIA5StringCodec;
import org.interledger.encoding.asn.codecs.AsnOctetStringCodec;
import org.interledger.encoding.asn.codecs.AsnSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;
import org.interledger.encoding.asn.codecs.AsnUint8Codec;

import org.interledger.btp.BtpSubProtocolContentType;

public class AsnBtpSubProtocolCodec extends AsnSequenceCodec<BtpSubProtocol> {

  public AsnBtpSubProtocolCodec() {
    super(
        new AsnIA5StringCodec(AsnSizeConstraint.UNCONSTRAINED),
        new AsnUint8Codec(),
        new AsnOctetStringCodec(AsnSizeConstraint.UNCONSTRAINED)
    );
  }

  /**
   * Decode and return the value read into the codec during serialization.
   *
   * @return the decoded object
   */
  @Override
  public BtpSubProtocol decode() {
    return BtpSubProtocol.builder()
        .protocolName(getValueAt(0))
        .contentType(BtpSubProtocolContentType.fromCode(getValueAt(1)))
        .data(getValueAt(2))
        .build();
  }

  /**
   * Encode the provided value into the codec to be written during serialization.
   *
   * @param value the value to encode
   */
  @Override
  public void encode(BtpSubProtocol value) {
    setValueAt(0, value.getProtocolName());
    setValueAt(1, value.getContentType().getCode());
    setValueAt(2, value.getData());
  }
}
