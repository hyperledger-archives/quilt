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

import org.interledger.stream.frames.StreamMoneyBlockedFrame;

import com.google.common.primitives.UnsignedLong;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

/**
 * Unit tests for {@link AsnStreamMoneyBlockedFrameCodec}.
 */
@RunWith(Parameterized.class)
public class AsnStreamMoneyBlockedFrameCodecTest extends AbstractAsnFrameCodecTest<StreamMoneyBlockedFrame> {

  public AsnStreamMoneyBlockedFrameCodecTest() {
    super(StreamMoneyBlockedFrame.class);
  }

  /**
   * The data for this test...
   */
  @Parameters
  public static Collection<Object[]> data() {

    return Arrays.asList(new Object[][] {
        // streamId=0
        {
            StreamMoneyBlockedFrame.builder()
                .streamId(UnsignedLong.ZERO)
                .sendMax(UnsignedLong.ONE)
                .totalSent(UnsignedLong.ONE)
                .build()
        },
        // streamId=1
        {
            StreamMoneyBlockedFrame.builder()
                .streamId(UnsignedLong.ONE)
                .sendMax(UnsignedLong.ONE)
                .totalSent(UnsignedLong.ONE)
                .build()
        },
        // streamId=10
        {
            StreamMoneyBlockedFrame.builder()
                .streamId(UnsignedLong.valueOf(10L))
                .sendMax(UnsignedLong.ONE)
                .totalSent(UnsignedLong.ONE)
                .build()
        },
        // streamId=Max-1
        {
            StreamMoneyBlockedFrame.builder()
                .streamId(UnsignedLong.MAX_VALUE.minus(UnsignedLong.ONE))
                .sendMax(UnsignedLong.ONE)
                .totalSent(UnsignedLong.ONE)
                .build()
        },
        // streamId=Max
        {
            StreamMoneyBlockedFrame.builder()
                .streamId(UnsignedLong.MAX_VALUE)
                .sendMax(UnsignedLong.ONE)
                .totalSent(UnsignedLong.ONE)
                .build()
        },
        // SendMax=0
        {
            StreamMoneyBlockedFrame.builder()
                .streamId(UnsignedLong.ONE)
                .sendMax(UnsignedLong.ZERO)
                .totalSent(UnsignedLong.ONE)
                .build()
        },
        // SendMax=1
        {
            StreamMoneyBlockedFrame.builder()
                .streamId(UnsignedLong.ONE)
                .sendMax(UnsignedLong.ONE)
                .totalSent(UnsignedLong.ONE)
                .build()
        },
        // SendMax=10
        {
            StreamMoneyBlockedFrame.builder()
                .streamId(UnsignedLong.ONE)
                .sendMax(UnsignedLong.valueOf(10L))
                .totalSent(UnsignedLong.ONE)
                .build()
        },
        // SendMax=Max-1
        {
            StreamMoneyBlockedFrame.builder()
                .streamId(UnsignedLong.ONE)
                .sendMax(UnsignedLong.MAX_VALUE.minus(UnsignedLong.ONE))
                .totalSent(UnsignedLong.ONE)
                .build()
        },
        // SendMax=Max
        {
            StreamMoneyBlockedFrame.builder()
                .streamId(UnsignedLong.ONE)
                .sendMax(UnsignedLong.MAX_VALUE)
                .totalSent(UnsignedLong.ONE)
                .build()
        },
        // totalSent=0
        {
            StreamMoneyBlockedFrame.builder()
                .streamId(UnsignedLong.ONE)
                .sendMax(UnsignedLong.ONE)
                .totalSent(UnsignedLong.ZERO)
                .build()
        },
        // totalSent=1
        {
            StreamMoneyBlockedFrame.builder()
                .streamId(UnsignedLong.ONE)
                .sendMax(UnsignedLong.ONE)
                .totalSent(UnsignedLong.ONE)
                .build()
        },
        // totalSent=10
        {
            StreamMoneyBlockedFrame.builder()
                .streamId(UnsignedLong.ONE)
                .sendMax(UnsignedLong.ONE)
                .totalSent(UnsignedLong.valueOf(10L))
                .build()
        },
        // totalSent=Max-1
        {
            StreamMoneyBlockedFrame.builder()
                .streamId(UnsignedLong.ONE)
                .sendMax(UnsignedLong.ONE)
                .totalSent(UnsignedLong.MAX_VALUE.minus(UnsignedLong.ONE))
                .build()
        },
        // totalSent=Max
        {
            StreamMoneyBlockedFrame.builder()
                .streamId(UnsignedLong.ONE)
                .sendMax(UnsignedLong.ONE)
                .totalSent(UnsignedLong.MAX_VALUE)
                .build()
        },

    });
  }
}
