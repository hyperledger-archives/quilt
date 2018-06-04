package org.interledger.btp;

/*-
 * ========================LICENSE_START=================================
 * Bilateral Transfer Protocol Core Libs
 * %%
 * Copyright (C) 2017 - 2018 Interledger
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */

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

    for (BtpErrorCode errorCode : BtpErrorCode.values()) {
      if (errorCode.code.equals(code)) {
        return errorCode;
      }
    }

    throw new IllegalArgumentException("Unknown BTP Error code: " + code);
  }

  public String getCode() {
    return code;
  }
}
