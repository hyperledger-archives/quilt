package org.interledger.btp;

/*-
 * ========================LICENSE_START=================================
 * Bilateral Transfer Protocol Core Libs
 * %%
 * Copyright (C) 2017 - 2018 Hyperledger and its contributors
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

import java.util.Objects;

/**
 * Holds information about BTP Errors.
 *
 * @see "https://github.com/interledger/rfcs/blob/master/0023-bilateral-transfer-protocol/0023-bilat
 *     eral-transfer-protocol.md#error-codes"
 */
public enum BtpErrorCode {

  /**
   * Temporary error, indicating that the connector cannot process this request at the moment. Try
   * again later.
   */
  T00_UnreachableError("T00", "UnreachableError",
      "Temporary error, indicating that the connector cannot process this request at the moment. "
          + "Try again later."),

  /**
   * Data were semantically invalid.
   */
  F00_NotAcceptedError("F00", "NotAcceptedError", "Data were semantically invalid."),

  /**
   * At least one field contained structurally invalid data, e.g. timestamp full of garbage
   * characters
   */
  F01_InvalidFieldsError("F01", "InvalidFieldsError",
      "At least one field contained structurally invalid data, e.g. timestamp full of garbage characters."),

  /**
   * The transferId included in the packet does not reference an existing transfer.
   */
  F03_TransferNotFoundError("F03", "TransferNotFoundError",
      "The transferId included in the packet does not reference an existing transfer."),

  /**
   * The fulfillment included in the packet does not match the transfer's condition.
   */
  F04_InvalidFulfillmentError("F04", "InvalidFulfillmentError",
      "The fulfillment included in the packet does not match the transfer's condition."),

  /**
   * The transferId and method match a previous request, but other data do not.
   */
  F05_DuplicateIdError("F05", "DuplicateIdError",
      "The transferId and method match a previous request, but other data do not."),

  /**
   * The transfer cannot be fulfilled because it has already been rejected or expired.
   */
  F06_AlreadyRolledBackError("F06", "AlreadyRolledBackError",
      "The transfer cannot be fulfilled because it has already been rejected or expired."),

  /**
   * The transfer cannot be rejected because it has already been fulfilled.
   */
  F07_AlreadyFulfilledError("F07", "AlreadyFulfilledError",
      "The transfer cannot be rejected because it has already been fulfilled."),

  /**
   * The transfer cannot be prepared because there is not enough available liquidity.
   */
  F08_InsufficientBalanceError("F08", "InsufficientBalanceError",
      "The transfer cannot be prepared because there is not enough available liquidity.");

  private final String codeIdentifier;

  private final String codeName;

  private final String codeDescription;

  BtpErrorCode(String codeIdentifier, String codeName, String codeDescription) {
    this.codeIdentifier = Objects.requireNonNull(codeIdentifier);
    this.codeName = Objects.requireNonNull(codeName);
    this.codeDescription = Objects.requireNonNull(codeDescription);
  }

  /**
   * Create a {@link BtpErrorCode} from the String representation of an error codeIdentifier.
   *
   * @param code The error codeIdentifier
   *
   * @return a new {@link BtpErrorCode}
   */
  public static BtpErrorCode fromCodeAsString(String code) {

    for (BtpErrorCode errorCode : BtpErrorCode.values()) {
      if (errorCode.codeIdentifier.equals(code)) {
        return errorCode;
      }
    }

    throw new IllegalArgumentException("Unknown BTP Error codeIdentifier: " + code);
  }

  public String getCodeIdentifier() {
    return codeIdentifier;
  }

  public String getCodeName() {
    return codeName;
  }

  public String getCodeDescription() {
    return codeDescription;
  }
}
