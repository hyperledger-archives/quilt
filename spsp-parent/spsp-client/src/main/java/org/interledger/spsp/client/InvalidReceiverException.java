package org.interledger.spsp.client;

/**
 * Exception for SPSP error: Receiver does not exist. Returned when an SPSP API call is performed on an account
 * that does not exist.
 */
public class InvalidReceiverException extends SpspException {

  public InvalidReceiverException(String url) {
    super("invalid receiver error calling " + url);
  }

}
