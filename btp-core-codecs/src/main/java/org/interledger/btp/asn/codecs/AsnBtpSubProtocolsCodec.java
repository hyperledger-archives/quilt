package org.interledger.btp.asn.codecs;

import org.interledger.btp.BtpSubProtocol;
import org.interledger.btp.BtpSubProtocols;
import org.interledger.encoding.asn.codecs.AsnSequenceOfSequenceCodec;

public class AsnBtpSubProtocolsCodec
    extends AsnSequenceOfSequenceCodec<BtpSubProtocols, BtpSubProtocol> {

  public AsnBtpSubProtocolsCodec() {
    super(BtpSubProtocols::new, AsnBtpSubProtocolCodec::new);
  }
}
