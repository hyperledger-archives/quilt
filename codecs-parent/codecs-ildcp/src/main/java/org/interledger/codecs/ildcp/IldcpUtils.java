package org.interledger.codecs.ildcp;

/*-
 * ========================LICENSE_START=================================
 * Interledger DCP Core Codecs
 * %%
 * Copyright (C) 2017 - 2019 Hyperledger and its contributors
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

import org.interledger.codecs.ildcp.IldcpCodecContextFactory;
import org.interledger.codecs.ildcp.IldcpCodecException;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.encoding.asn.framework.CodecException;
import org.interledger.ildcp.IldcpResponse;
import org.interledger.ildcp.IldcpResponsePacket;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

/**
 * Helper class to marshal and unmarshal instances of {@link IldcpResponse} from an Interledger packet.
 */
public class IldcpUtils {

  /**
   * Converts an {@link IldcpResponse} to a corresponding {@link IldcpResponsePacket} packet by encoding the {@code
   * ildcpResponse} into the returned packet's `data` payload.
   *
   * @param ildcpResponse A {@link IldcpResponse} to encode and package into the `data` property of a new Prepare
   *                      packet.
   *
   * @return A {@link InterledgerPreparePacket} that conforms to the IL-DCP RFC.
   */
  public static IldcpResponsePacket fromIldcpResponse(final IldcpResponse ildcpResponse) {
    Objects.requireNonNull(ildcpResponse);

    // Convert IldcpResponse to bytes...
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    try {
      IldcpCodecContextFactory.oer().write(ildcpResponse, os);
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }

    return IldcpResponsePacket.builder()
        .ildcpResponse(ildcpResponse)
        .data(os.toByteArray())
        .build();
  }

  /**
   * Converts an {@link IldcpResponse} to a corresponding ILP Fulfillment packet.
   *
   * @param packet A {@link InterledgerPreparePacket} to decode into an  IL-DCP response.
   *
   * @return A {@link IldcpResponse} that conforms to the IL-DCP RFC.
   */
  public static IldcpResponse toIldcpResponse(final InterledgerFulfillPacket packet) {
    Objects.requireNonNull(packet);

    // Convert IldcpResponse to bytes...
    try {
      final ByteArrayInputStream is = new ByteArrayInputStream(packet.getData());
      return IldcpCodecContextFactory.oer().read(IldcpResponse.class, is);
    } catch (CodecException e) {
      throw new IldcpCodecException(
          "Packet must have a data payload containing an encoded instance of IldcpResponse", e
      );
    } catch (IOException e) {
      throw new IldcpCodecException(
          "Packet must have a data payload containing an encoded instance of IldcpResponse", e);
    }
  }

}
