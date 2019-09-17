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

import org.interledger.core.InterledgerAddress;
import org.interledger.ildcp.IldcpResponse;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AsnIldcpResponseCodec}.
 */
public class AsnIldcpResponseCodecTest {

  private static final InterledgerAddress FOO_ADDRESS = InterledgerAddress.of("example.foo");
  private static final String BTC = "BTC";

  private AsnIldcpResponseCodec codec;

  private IldcpResponse RESPONSE = IldcpResponse.builder()
      .clientAddress(FOO_ADDRESS)
      .assetScale((short) 9)
      .assetCode(BTC)
      .build();

  @Before
  public void setUp() {
    codec = new AsnIldcpResponseCodec();
  }

  @Test
  public void decode() {
    codec.encode(RESPONSE);
    assertThat(codec.getCodecAt(0).decode()).isEqualTo(FOO_ADDRESS);
    assertThat(codec.getCodecAt(1).decode()).isEqualTo((short) 9);
    assertThat(codec.getCodecAt(2).decode()).isEqualTo(BTC);
  }

  @Test
  public void encode() {
    codec.encode(RESPONSE);
    final IldcpResponse actual = codec.decode();
    assertThat(actual).isEqualTo(RESPONSE);
  }

  @Test
  public void readWrite() throws IOException {
    // Write
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    IldcpCodecContextFactory.oer().write(RESPONSE, os);
    assertThat(Base64.getEncoder().encodeToString(os.toByteArray())).isEqualTo("C2V4YW1wbGUuZm9vCQNCVEM=");

    // Read
    final ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
    final IldcpResponse decodedValue = IldcpCodecContextFactory.oer().read(IldcpResponse.class, is);
    assertThat(decodedValue).isEqualTo(RESPONSE);
  }
}
