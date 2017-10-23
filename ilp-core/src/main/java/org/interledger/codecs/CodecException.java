package org.interledger.codecs;

import org.interledger.InterledgerRuntimeException;

/**
 * An extension of {@link RuntimeException} to represent errors that occur during encoding or
 * decoding.
 */
public class CodecException extends InterledgerRuntimeException {

  private static final long serialVersionUID = 6647367875148981736L;

  public CodecException(String message) {
    super(message);
  }

  public CodecException(String message, Throwable cause) {
    super(message, cause);
  }

  public CodecException(Throwable cause) {
    super(cause);
  }
}
