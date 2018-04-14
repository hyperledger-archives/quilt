package org.interledger.btp.asn.codecs;

import org.interledger.btp.BtpResponse;

public class AsnBtpResponseDataCodec extends AsnBtpPacketDataCodec<BtpResponse> {


  public AsnBtpResponseDataCodec(long requestId) {
    super(
        requestId,
        new AsnBtpSubProtocolsCodec() //SubProtocols
    );
  }

  /**
   * Decode and return the value read into the codec during serialization.
   *
   * @return the decoded object
   */
  @Override
  public BtpResponse decode() {
    return BtpResponse.builder()
        .requestId(getRequestId())
        .subProtocols(getValueAt(0))
        .build();
  }

  /**
   * Encode the provided value into the codec to be written during serialization.
   *
   * @param value the value to encode
   */
  @Override
  public void encode(BtpResponse value) {
    setValueAt(0, value.getSubProtocols());
  }

}
