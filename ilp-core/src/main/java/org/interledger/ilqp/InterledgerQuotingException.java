package org.interledger.ilqp;

import org.interledger.InterledgerRuntimeException;

public class InterledgerQuotingException extends InterledgerRuntimeException {

  private static final long serialVersionUID = 6272999499660262013L;

  public InterledgerQuotingException(String message) {
    super(message);
  }

  public InterledgerQuotingException(String message, Throwable cause) {
    super(message, cause);
  }

}
