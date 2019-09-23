package org.interledger.spsp.client;

public class SpspException extends RuntimeException {

  public SpspException(String message, Throwable cause) {
    super(message, cause);
  }

  public SpspException(String message) {
    super(message);
  }

}
