package org.interledger.codecs.btp;

/*-
 * ========================LICENSE_START=================================
 * Bilateral Transfer Protocol Core Codecs
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

import org.junit.Before;
import org.junit.Test;

import java.time.Instant;

/**
 * Unit tests for {@link AsnBtpGeneralizedTimeCodec}.
 */
public class AsnBtpGeneralizedTimeCodecTest {

  private AsnBtpGeneralizedTimeCodec codec;

  @Before
  public void setup() {
    this.codec = new AsnBtpGeneralizedTimeCodec();
  }

  @Test
  public void encodeDecode() {
    final Instant initial = Instant.parse("2018-12-24T11:59:23Z");
    codec.encode(initial);
    assertThat(codec.getCharString()).isEqualTo("20181224115923Z");
    final Instant actual = codec.decode();
    assertThat(actual).isEqualTo(initial);
  }
}
