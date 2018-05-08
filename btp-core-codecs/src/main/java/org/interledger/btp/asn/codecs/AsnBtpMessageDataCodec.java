package org.interledger.btp.asn.codecs;

import org.interledger.btp.BtpMessage;

public class AsnBtpMessageDataCodec extends AsnBtpPacketDataCodec<BtpMessage> {


  /**
   * Default constructor.
   *
   * @param requestId the correlation id of the message/response
   */
  public AsnBtpMessageDataCodec(long requestId) {
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
  public BtpMessage decode() {
    return BtpMessage.builder()
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
  public void encode(BtpMessage value) {
    setValueAt(0, value.getSubProtocols());
  }

}
