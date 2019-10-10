/*-
 * ========================LICENSE_START=================================
 * Interledger Core Codecs
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

package org.interledger.codecs.stream.frame;

import org.interledger.stream.frames.ConnectionMaxStreamIdFrame;

import com.google.common.primitives.UnsignedLong;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

/**
 * Unit tests for {@link AsnConnectionMaxStreamIdFrameCodec}.
 */
@RunWith(Parameterized.class)
public class AsnConnectionMaxStreamIdFrameCodecTest extends AbstractAsnFrameCodecTest<ConnectionMaxStreamIdFrame> {

  public AsnConnectionMaxStreamIdFrameCodecTest() {
    super(ConnectionMaxStreamIdFrame.class);
  }

  /**
   * The data for this test...
   */
  @Parameters
  public static Collection<Object[]> data() {

    return Arrays.asList(new Object[][] {
        // maxStreamId = 0
        {
            ConnectionMaxStreamIdFrame.builder()
                .maxStreamId(UnsignedLong.ZERO)
                .build()
        },
        // maxStreamId = 1
        {
            ConnectionMaxStreamIdFrame.builder()
                .maxStreamId(UnsignedLong.ONE)
                .build()
        },
        // maxStreamId = 10
        {
            ConnectionMaxStreamIdFrame.builder()
                .maxStreamId(UnsignedLong.valueOf(10L))
                .build()
        },
        // maxStreamId = Long.Max - 1
        {
            ConnectionMaxStreamIdFrame.builder()
                .maxStreamId(UnsignedLong.MAX_VALUE.minus(UnsignedLong.ONE))
                .build()
        },
        // maxStreamId = Long.Max
        {
            ConnectionMaxStreamIdFrame.builder()
                .maxStreamId(UnsignedLong.MAX_VALUE)
                .build()
        },
    });
  }
}
