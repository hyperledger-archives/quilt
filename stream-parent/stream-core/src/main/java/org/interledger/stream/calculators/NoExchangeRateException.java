package org.interledger.stream.calculators;

/**
 * Thrown when an exchange rate could not be determined. This could happen if the destination denomination is
 * unknown or if an exchange rate cannot be found to the destination denomination.
 */
public class NoExchangeRateException extends RuntimeException {

  public NoExchangeRateException(String message) {
    super(message);
  }

}
