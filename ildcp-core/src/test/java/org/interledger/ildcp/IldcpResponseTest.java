package org.interledger.ildcp;

/*-
 * ========================LICENSE_START=================================
 * Interledger DCP Core
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

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.core.InterledgerAddress;

import org.junit.Test;

/**
 * Unit tests for {@link IldcpResponse}.
 */
public class IldcpResponseTest {

  private static final InterledgerAddress FOO_ADDRESS = InterledgerAddress.of("example.foo");
  private static final String BTC = "BTC";

  @Test(expected = IllegalStateException.class)
  public void testBuilderWhenEmpty() {
    final String errorMessage =
        "Cannot build IldcpResponse, some of required attributes are not set [clientAddress, assetCode, assetScale]";
    try {
      IldcpResponse.builder().build();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo(errorMessage);
      throw e;
    }
  }

  @Test
  public void testBuilder() {
    final IldcpResponse response = IldcpResponse.builder()
        .clientAddress(FOO_ADDRESS)
        .assetScale((short) 9)
        .assetCode(BTC)
        .build();

    assertThat(response.getClientAddress()).isEqualTo(FOO_ADDRESS);
    assertThat(response.getAssetScale()).isEqualTo((short) 9);
    assertThat(response.getAssetCode()).isEqualTo(BTC);
  }
}
