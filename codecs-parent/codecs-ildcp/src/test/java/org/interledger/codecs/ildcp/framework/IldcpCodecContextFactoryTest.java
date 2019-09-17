package org.interledger.codecs.ildcp.framework;

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

import org.interledger.codecs.ildcp.AsnIldcpResponseCodec;
import org.interledger.codecs.ildcp.IldcpCodecContextFactory;
import org.interledger.core.InterledgerAddress;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.encoding.asn.framework.CodecContextFactory;
import org.interledger.ildcp.IldcpRequestPacket;
import org.interledger.ildcp.IldcpResponse;
import org.interledger.ildcp.IldcpResponsePacket;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link IldcpCodecContextFactory}.
 */
public class IldcpCodecContextFactoryTest {

  private static final InterledgerAddress FOO_ADDRESS = InterledgerAddress.of("example.foo");
  private static final String BTC = "BTC";
  private static final Instant NOW = Instant.parse("2019-12-25T01:02:03.996Z");

  private static final IldcpResponse TEST_RESPONSE = IldcpResponse.builder()
      .clientAddress(FOO_ADDRESS)
      .assetScale((short) 9)
      .assetCode(BTC)
      .build();

  @Test
  public void testReadWriteRequestPacket() throws IOException {
    final IldcpRequestPacket requestPacket = IldcpRequestPacket.builder().expiresAt(NOW).build();

    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    IldcpCodecContextFactory.oer().write(requestPacket, os);

    final String base64Bytes = Base64.getEncoder().encodeToString(os.toByteArray());
    assertThat(base64Bytes).isEqualTo("DEYAAAAAAAAAADIwMTkxMjI1MDEwMjAzOTk2Zmh6rfhivXdsj8GLjp+OIAiXFIVu4jOzkCpZHQ1fKSULcGVlci5jb25maWcA");

    final IldcpRequestPacket decodedPacket = IldcpCodecContextFactory.oer()
        .read(IldcpRequestPacket.class, new ByteArrayInputStream(os.toByteArray()));
    assertThat(decodedPacket).isEqualTo(requestPacket);
  }


  @Test
  public void testReadWriteResponsePacket() throws IOException {
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    final IldcpResponsePacket responsePacket = IldcpResponsePacket.builder().ildcpResponse(TEST_RESPONSE).build();
    IldcpCodecContextFactory.oer().write(responsePacket, os);

    final IldcpResponsePacket decodedResponsePacket = IldcpCodecContextFactory.oer()
        .read(IldcpResponsePacket.class, new ByteArrayInputStream(os.toByteArray()));

    assertThat(decodedResponsePacket).isEqualTo(responsePacket);
  }

  @Test
  public void register() throws IOException {
    final CodecContext codecContext = CodecContextFactory.oer();
    codecContext.register(IldcpResponse.class, AsnIldcpResponseCodec::new);

    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    IldcpCodecContextFactory.oer().write(TEST_RESPONSE, os);

    final String base64Bytes = Base64.getEncoder().encodeToString(os.toByteArray());
    assertThat(base64Bytes).isEqualTo("C2V4YW1wbGUuZm9vCQNCVEM=");
  }
}
