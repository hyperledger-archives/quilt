package org.interledger.stream.crypto;

public class EncryptionException extends RuntimeException {

  public EncryptionException() {
  }

  public EncryptionException(String message) {
    super(message);
  }

  public EncryptionException(String message, Throwable cause) {
    super(message, cause);
  }

  public EncryptionException(Throwable cause) {
    super(cause);
  }
}
