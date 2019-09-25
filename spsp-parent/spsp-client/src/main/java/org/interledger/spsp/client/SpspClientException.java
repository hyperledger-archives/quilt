package org.interledger.spsp.client;

public class SpspClientException extends RuntimeException {

  public SpspClientException(String message, Throwable cause) {
    super(message, cause);
  }

  public SpspClientException(String message) {
    super(message);
  }

}
