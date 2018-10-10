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

import org.interledger.annotations.Immutable;

import org.immutables.value.Value.Default;

import java.nio.charset.StandardCharsets;

/**
 * Contains information required to process sub-protocols using BTP.
 */
public interface BtpSubProtocol {

  static BtpSubProtocolBuilder builder() {
    return new BtpSubProtocolBuilder();
  }

  /**
   * The name of this side protocol. ILP-level information must be named ilp.
   */
  String getProtocolName();

  /**
   * The content-type of this sub-protocol.
   */
  default BtpSubProtocolContentType getContentType() {
    return BtpSubProtocolContentType.MIME_APPLICATION_OCTET_STREAM;
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
   * Abstract implementation to satisfy the Immutables library.
   */
  @Immutable
  abstract class AbstractBtpSubProtocol implements BtpSubProtocol {

    @Default
    public BtpSubProtocolContentType getContentType() {
      return BtpSubProtocolContentType.MIME_APPLICATION_OCTET_STREAM;
    }

    @Default
    public byte[] getData() {
      return new byte[0];
    }
  }

}
