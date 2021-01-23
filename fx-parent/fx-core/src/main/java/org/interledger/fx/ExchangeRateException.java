package org.interledger.fx;

/**
 * The top-level exception thrown by FX infrastructure in Quilt.
 */
public class ExchangeRateException extends RuntimeException {

  public ExchangeRateException() {
    super();
  }

  public ExchangeRateException(String message) {
    super(message);
  }

  public ExchangeRateException(String message, Throwable cause) {
    super(message, cause);
  }

  public ExchangeRateException(Throwable cause) {
    super(cause);
  }

  protected ExchangeRateException(
    String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace
  ) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
