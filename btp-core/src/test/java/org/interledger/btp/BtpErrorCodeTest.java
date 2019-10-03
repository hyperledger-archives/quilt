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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class BtpErrorCodeTest {

  @Test
  public void fromString() {

    for (BtpErrorCode code : BtpErrorCode.values()) {
      assertEquals(code, BtpErrorCode.fromCodeAsString(code.getCodeIdentifier()));
    }
  }

  @Test
  public void testBtpErrorCodeDescriptions() {
    final BtpErrorCode[] errorCodes = BtpErrorCode.values();

    Map<String, String> errorToDescriptionMap = new HashMap<>();
    errorToDescriptionMap.put(BtpErrorCode.F00_NotAcceptedError.getCodeName(),
        BtpErrorCode.F00_NotAcceptedError.getCodeDescription());
    errorToDescriptionMap.put(BtpErrorCode.F01_InvalidFieldsError.getCodeName(),
        BtpErrorCode.F01_InvalidFieldsError.getCodeDescription());
    errorToDescriptionMap.put(BtpErrorCode.F03_TransferNotFoundError.getCodeName(),
        BtpErrorCode.F03_TransferNotFoundError.getCodeDescription());
    errorToDescriptionMap.put(BtpErrorCode.F04_InvalidFulfillmentError.getCodeName(),
        BtpErrorCode.F04_InvalidFulfillmentError.getCodeDescription());
    errorToDescriptionMap.put(BtpErrorCode.F05_DuplicateIdError.getCodeName(),
        BtpErrorCode.F05_DuplicateIdError.getCodeDescription());
    errorToDescriptionMap.put(BtpErrorCode.F06_AlreadyRolledBackError.getCodeName(),
        BtpErrorCode.F06_AlreadyRolledBackError.getCodeDescription());
    errorToDescriptionMap.put(BtpErrorCode.F07_AlreadyFulfilledError.getCodeName(),
        BtpErrorCode.F07_AlreadyFulfilledError.getCodeDescription());
    errorToDescriptionMap.put(BtpErrorCode.F08_InsufficientBalanceError.getCodeName(),
        BtpErrorCode.F08_InsufficientBalanceError.getCodeDescription());
    errorToDescriptionMap.put(BtpErrorCode.T00_UnreachableError.getCodeName(),
        BtpErrorCode.T00_UnreachableError.getCodeDescription());

    assertEquals(errorCodes.length, errorToDescriptionMap.size());
    for (BtpErrorCode code : errorCodes) {
      assertEquals(code.getCodeDescription(), errorToDescriptionMap.get(code.getCodeName()));
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void fromBadString() {
    BtpErrorCode.fromCodeAsString("TEST");
  }

  @Test(expected = IllegalArgumentException.class)
  public void fromEmptyString() {
    BtpErrorCode.fromCodeAsString("");
  }

  @Test(expected = IllegalArgumentException.class)
  public void fromNullString() {
    BtpErrorCode.fromCodeAsString(null);
  }

}
