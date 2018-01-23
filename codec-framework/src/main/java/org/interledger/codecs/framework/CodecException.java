package org.interledger.codecs.framework;

/**
 * An extension of {@link RuntimeException} to represent errors that occur during encoding or
 * decoding.
 */
public class CodecException extends RuntimeException {

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
