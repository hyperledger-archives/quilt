package org.interledger.btp.asn.codecs;

import org.interledger.btp.BtpSubProtocol;
import org.interledger.encoding.asn.codecs.AsnSequenceOfSequenceCodec;

import org.interledger.btp.BtpSubProtocols;

public class AsnBtpSubProtocolsCodec
    extends AsnSequenceOfSequenceCodec<BtpSubProtocols, BtpSubProtocol> {

  public AsnBtpSubProtocolsCodec() {
    super(BtpSubProtocols::new, AsnBtpSubProtocolCodec::new);
  }
}
