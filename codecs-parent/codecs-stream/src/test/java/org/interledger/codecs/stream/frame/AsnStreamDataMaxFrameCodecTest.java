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

import org.interledger.stream.frames.StreamDataMaxFrame;

import com.google.common.primitives.UnsignedLong;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

/**
 * Unit tests for {@link AsnStreamDataMaxFrameCodec}.
 */
@RunWith(Parameterized.class)
public class AsnStreamDataMaxFrameCodecTest extends AbstractAsnFrameCodecTest<StreamDataMaxFrame> {

  public AsnStreamDataMaxFrameCodecTest() {
    super(StreamDataMaxFrame.class);
  }

  /**
   * The data for this test...
   */
  @Parameters
  public static Collection<Object[]> data() {

    return Arrays.asList(new Object[][] {
        // streamId=0
        {
            StreamDataMaxFrame.builder()
                .streamId(UnsignedLong.ZERO)
                .maxOffset(UnsignedLong.valueOf(10L))
                .build()
        },
        // streamId=1
        {
            StreamDataMaxFrame.builder()
                .streamId(UnsignedLong.ONE)
                .maxOffset(UnsignedLong.valueOf(10L))
                .build()
        },
        // streamId=10
        {
            StreamDataMaxFrame.builder()
                .streamId(UnsignedLong.valueOf(10L))
                .maxOffset(UnsignedLong.valueOf(10L))
                .build()
        },
        // streamId=Max-1
        {
            StreamDataMaxFrame.builder()
                .streamId(UnsignedLong.MAX_VALUE.minus(UnsignedLong.ONE))
                .maxOffset(UnsignedLong.valueOf(10L))
                .build()
        },
        // streamId=Max
        {
            StreamDataMaxFrame.builder()
                .streamId(UnsignedLong.MAX_VALUE)
                .maxOffset(UnsignedLong.valueOf(10L))
                .build()
        },
        // MaxOffset=0
        {
            StreamDataMaxFrame.builder()
                .streamId(UnsignedLong.ONE)
                .maxOffset(UnsignedLong.ZERO)
                .build()
        },
        // MaxOffset=1
        {
            StreamDataMaxFrame.builder()
                .streamId(UnsignedLong.ONE)
                .maxOffset(UnsignedLong.ONE)
                .build()
        },
        // MaxOffset=10
        {
            StreamDataMaxFrame.builder()
                .streamId(UnsignedLong.ONE)
                .maxOffset(UnsignedLong.valueOf(10L))
                .build()
        },
        // MaxOffset=Max-1
        {
            StreamDataMaxFrame.builder()
                .streamId(UnsignedLong.ONE)
                .maxOffset(UnsignedLong.MAX_VALUE.minus(UnsignedLong.ONE))
                .build()
        },
        // MaxOffset=Max
        {
            StreamDataMaxFrame.builder()
                .streamId(UnsignedLong.ONE)
                .maxOffset(UnsignedLong.MAX_VALUE)
                .build()
        },

    });
  }

}
