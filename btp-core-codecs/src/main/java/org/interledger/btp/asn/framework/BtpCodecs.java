package org.interledger.btp.asn.framework;

import org.interledger.btp.BtpError;
import org.interledger.btp.BtpMessage;
import org.interledger.btp.BtpPacket;
import org.interledger.btp.BtpResponse;
import org.interledger.btp.BtpSubProtocol;
import org.interledger.btp.BtpSubProtocols;
import org.interledger.btp.BtpTransfer;
import org.interledger.btp.asn.codecs.AsnBtpErrorCodec;
import org.interledger.btp.asn.codecs.AsnBtpMessageCodec;
import org.interledger.btp.asn.codecs.AsnBtpPacketCodec;
import org.interledger.btp.asn.codecs.AsnBtpResponseCodec;
import org.interledger.btp.asn.codecs.AsnBtpSubProtocolCodec;
import org.interledger.btp.asn.codecs.AsnBtpSubProtocolsCodec;
import org.interledger.btp.asn.codecs.AsnBtpTransferCodec;
import org.interledger.encoding.asn.framework.CodecContext;

public class BtpCodecs {

  /**
   * Register the BTP protocol codecs into the provided context.
   *
   * @param context the context to register the codecs into
   */
  public static void register(CodecContext context) {
    context
        .register(BtpError.class, AsnBtpErrorCodec::new)
        .register(BtpMessage.class, AsnBtpMessageCodec::new)
        .register(BtpPacket.class, AsnBtpPacketCodec::new)
        .register(BtpResponse.class, AsnBtpResponseCodec::new)
        .register(BtpSubProtocol.class, AsnBtpSubProtocolCodec::new)
        .register(BtpSubProtocols.class, AsnBtpSubProtocolsCodec::new)
        .register(BtpTransfer.class, AsnBtpTransferCodec::new);
  }

}
