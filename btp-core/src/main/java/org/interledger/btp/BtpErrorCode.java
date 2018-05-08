package org.interledger.btp;

public enum BtpErrorCode {

  /**
   * Temporary error, indicating that the connector cannot process this request at the moment. Try again later.
   */
  T00_UnreachableError("T00"),
  /**
   * Data were symantically invalid.
   */
  F00_NotAcceptedError("F00"),
  /**
   * At least one field contained structurally invalid data, e.g. timestamp full of garbage characters
   */
  F01_InvalidFieldsError("F01"),
  /**
   * The transferId included in the packet does not reference an existing transfer.
   */
  F03_TransferNotFoundError("F03"),
  /**
   * The fulfillment included in the packet does not match the transfer's condition.
   */
  F04_InvalidFulfillmentError("F04"),
  /**
   * The transferId and method match a previous request, but other data do not.
   */
  F05_DuplicateIdError("F05"),
  /**
   * The transfer cannot be fulfilled because it has already been rejected or expired.
   */
  F06_AlreadyRolledBackError("F06"),
  /**
   * The transfer cannot be rejected because it has already been fulfilled.
   */
  F07_AlreadyFulfilledError("F07"),
  /**
   * The transfer cannot be prepared because there is not enough available liquidity.
   */
  F08_InsufficientBalanceError("F08");

  private final String code;

  BtpErrorCode(String code) {
    this.code = code;
  }

  /**
   * Create a {@link BtpErrorCode} from the Sring representation of an error code.
   *
   * @param code The error code
   * @return a new {@link BtpErrorCode}
   */
  public static BtpErrorCode fromString(String code) {

    for (BtpErrorCode errorCode :
        BtpErrorCode.values()) {
      if (errorCode.code.equals(code)) {
        return errorCode;
      }
    }

    throw new RuntimeException("Unknown code: " + code);
  }

  public String getCode() {
    return code;
  }
}
