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

import static java.lang.String.format;

public enum BtpSubProtocolContentType {
  MIME_APPLICATION_OCTET_STREAM((short) 0),
  MIME_TEXT_PLAIN_UTF8((short) 1),
  MIME_APPLICATION_JSON((short) 2),
  ;

  private final short code;


  BtpSubProtocolContentType(short code) {
    this.code = code;
  }

  /**
   * Get a new {@link BtpSubProtocolContentType} from the given code.
   *
   * @param code a type code
   *
   * @return an instance of {@link BtpSubProtocolContentType}
   */
  public static BtpSubProtocolContentType fromCode(short code) {

    switch (code) {
      case 0:
        return MIME_APPLICATION_OCTET_STREAM;
      case 1:
        return MIME_TEXT_PLAIN_UTF8;
      case 2:
        return MIME_APPLICATION_JSON;
      default:
        throw new IllegalArgumentException(
            format("Unknown BTP Sub-Protocol Content Type: %s", code));
    }
  }

  public short getCode() {
    return this.code;
  }
}
