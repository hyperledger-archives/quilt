package org.interledger.btp.asn.codecs;

import org.interledger.btp.BtpTransfer;
import org.interledger.encoding.asn.codecs.AsnUint64Codec;

/**
 * Used to encode/decode the body of a MESSAGE and RESPONSE
 */
public class AsnBtpTransferDataCodec extends AsnBtpPacketDataCodec<BtpTransfer> {


  public AsnBtpTransferDataCodec(long requestId) {
    super(
        requestId,
        new AsnUint64Codec(), //Amount
        new AsnBtpSubProtocolsCodec() //SubProtocols
    );
  }

  /**
   * Decode and return the value read into the codec during serialization.
   *
   * @return the decoded object
   */
  @Override
  public BtpTransfer decode() {
    return BtpTransfer.builder()
        .amount(getValueAt(0))
        .subProtocols(getValueAt(1))
        .build();
  }

  /**
   * Encode the provided value into the codec to be written during serialization.
   *
   * @param value the value to encode
   */
  @Override
  public void encode(BtpTransfer value) {
    setValueAt(0, value.getAmount());
    setValueAt(1, value.getSubProtocols());
  }

}
