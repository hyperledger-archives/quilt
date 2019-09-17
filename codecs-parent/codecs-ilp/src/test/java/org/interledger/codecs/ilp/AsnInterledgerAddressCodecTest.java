package org.interledger.codecs.ilp;

/*-
 * ========================LICENSE_START=================================
 * Interledger Core Codecs
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

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AsnInterledgerAddressCodec}.
 */
public class AsnInterledgerAddressCodecTest {

  private static final String G_FOO = "g.foo";
  private AsnInterledgerAddressCodec codec;

  @Before
  public void setUp() {
    codec = new AsnInterledgerAddressCodec();
    codec.setCharString(G_FOO);
  }

  @Test
  public void decode() {
    assertThat(codec.decode()).isEqualTo(InterledgerAddress.of(G_FOO));
  }

  @Test
  public void encode() {
    codec.encode(InterledgerAddress.of(G_FOO));
    assertThat(codec.getCharString()).isEqualTo(G_FOO);
  }
}
