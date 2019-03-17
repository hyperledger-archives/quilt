package org.interledger.ildcp.asn.codecs;

import org.interledger.encoding.asn.framework.CodecException;

/**
 * A {@link CodecException} thrown for IL-DCP codec errors.
 */
public class IldcpCodecException extends CodecException {

  public IldcpCodecException(String message) {
    super(message);
  }

  public IldcpCodecException(String message, Throwable cause) {
    super(message, cause);
  }

  public IldcpCodecException(Throwable cause) {
    super(cause);
  }
}
