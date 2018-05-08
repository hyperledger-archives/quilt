package org.interledger.ildcp.asn.framework;

import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.ildcp.IldcpResponse;
import org.interledger.ildcp.asn.codecs.AsnIldcpResponseCodec;

public class IldcpCodecs {

  public static void register(CodecContext context) {
    context.register(IldcpResponse.class, AsnIldcpResponseCodec::new);
  }

}
