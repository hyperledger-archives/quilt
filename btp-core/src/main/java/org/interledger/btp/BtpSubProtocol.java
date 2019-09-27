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

import org.interledger.core.Immutable;

import org.immutables.value.Value.Default;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Contains information required to process sub-protocols using BTP.
 */
public interface BtpSubProtocol {

  static BtpSubProtocolBuilder builder() {
    return new BtpSubProtocolBuilder();
  }

  /**
   * The name of this side protocol. ILP-level information must be named toBtpSubprotocol.
   */
  String getProtocolName();

  /**
   * The content-type of this sub-protocol.
   */
  default ContentType getContentType() {
    return ContentType.MIME_APPLICATION_OCTET_STREAM;
  }

  /**
   * The actual protocol data for this sub-protocol.
   */
  default byte[] getData() {
    return new byte[0];
  }

  default String getDataAsString() {
    return new String(getData(), StandardCharsets.UTF_8);
  }

  /**
   * A content-type descriptor for BTP sub-protocol payloads, mainly used for logging and smart deserializing.
   */
  enum ContentType {
    MIME_APPLICATION_OCTET_STREAM((short) 0),
    MIME_TEXT_PLAIN_UTF8((short) 1),
    MIME_APPLICATION_JSON((short) 2),
    ;

    private final short code;


    ContentType(short code) {
      this.code = code;
    }

    /**
     * Get a new {@link ContentType} from the given code.
     *
     * @param code a type code
     *
     * @return an instance of {@link ContentType}
     */
    public static ContentType fromCode(short code) {

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

  /**
   * Abstract implementation to satisfy the Immutables library.
   */
  @Immutable
  abstract class AbstractBtpSubProtocol implements BtpSubProtocol {

    @Default
    public ContentType getContentType() {
      return ContentType.MIME_APPLICATION_OCTET_STREAM;
    }

    @Default
    public byte[] getData() {
      return new byte[0];
    }

    /**
     * Prints the immutable value {@code BtpSubProtocol} with attribute values.
     *
     * @return A string representation of the value
     */
    @Override
    public String toString() {
      return "BtpSubProtocol{"
          + "contentType=" + getContentType()
          + ", protocolName=" + getProtocolName()
          + ", data=" + Base64.getEncoder().encodeToString(getData())
          + "}";
    }

  }

}
