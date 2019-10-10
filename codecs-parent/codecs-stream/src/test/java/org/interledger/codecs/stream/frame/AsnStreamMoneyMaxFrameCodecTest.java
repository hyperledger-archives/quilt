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

import org.interledger.stream.frames.StreamMoneyMaxFrame;

import com.google.common.primitives.UnsignedLong;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

/**
 * Unit tests for {@link AsnStreamMoneyMaxFrameCodec}.
 */
@RunWith(Parameterized.class)
public class AsnStreamMoneyMaxFrameCodecTest extends AbstractAsnFrameCodecTest<StreamMoneyMaxFrame> {

  public AsnStreamMoneyMaxFrameCodecTest() {
    super(StreamMoneyMaxFrame.class);
  }

  /**
   * The data for this test...
   */
  @Parameters
  public static Collection<Object[]> data() {

    return Arrays.asList(new Object[][] {
        // streamId=0
        {
            StreamMoneyMaxFrame.builder()
                .streamId(UnsignedLong.ZERO)
                .receiveMax(UnsignedLong.ONE)
                .totalReceived(UnsignedLong.ONE)
                .build()
        },
        // streamId=1
        {
            StreamMoneyMaxFrame.builder()
                .streamId(UnsignedLong.ONE)
                .receiveMax(UnsignedLong.ONE)
                .totalReceived(UnsignedLong.ONE)
                .build()
        },
        // streamId=10
        {
            StreamMoneyMaxFrame.builder()
                .streamId(UnsignedLong.valueOf(10L))
                .receiveMax(UnsignedLong.ONE)
                .totalReceived(UnsignedLong.ONE)
                .build()
        },
        // streamId=Max-1
        {
            StreamMoneyMaxFrame.builder()
                .streamId(UnsignedLong.MAX_VALUE.minus(UnsignedLong.ONE))
                .receiveMax(UnsignedLong.ONE)
                .totalReceived(UnsignedLong.ONE)
                .build()
        },
        // streamId=Max
        {
            StreamMoneyMaxFrame.builder()
                .streamId(UnsignedLong.MAX_VALUE)
                .receiveMax(UnsignedLong.ONE)
                .totalReceived(UnsignedLong.ONE)
                .build()
        },
        // ReceiveMax=0
        {
            StreamMoneyMaxFrame.builder()
                .streamId(UnsignedLong.ONE)
                .receiveMax(UnsignedLong.ZERO)
                .totalReceived(UnsignedLong.ONE)
                .build()
        },
        // ReceiveMax=1
        {
            StreamMoneyMaxFrame.builder()
                .streamId(UnsignedLong.ONE)
                .receiveMax(UnsignedLong.ONE)
                .totalReceived(UnsignedLong.ONE)
                .build()
        },
        // ReceiveMax=10
        {
            StreamMoneyMaxFrame.builder()
                .streamId(UnsignedLong.ONE)
                .receiveMax(UnsignedLong.valueOf(10L))
                .totalReceived(UnsignedLong.ONE)
                .build()
        },
        // ReceiveMax=Max-1
        {
            StreamMoneyMaxFrame.builder()
                .streamId(UnsignedLong.ONE)
                .receiveMax(UnsignedLong.MAX_VALUE.minus(UnsignedLong.ONE))
                .totalReceived(UnsignedLong.ONE)
                .build()
        },
        // ReceiveMax=Max
        {
            StreamMoneyMaxFrame.builder()
                .streamId(UnsignedLong.ONE)
                .receiveMax(UnsignedLong.MAX_VALUE)
                .totalReceived(UnsignedLong.ONE)
                .build()
        },
        // totalReceived=0
        {
            StreamMoneyMaxFrame.builder()
                .streamId(UnsignedLong.ONE)
                .receiveMax(UnsignedLong.ONE)
                .totalReceived(UnsignedLong.ZERO)
                .build()
        },
        // totalReceived=1
        {
            StreamMoneyMaxFrame.builder()
                .streamId(UnsignedLong.ONE)
                .receiveMax(UnsignedLong.ONE)
                .totalReceived(UnsignedLong.ONE)
                .build()
        },
        // totalReceived=10
        {
            StreamMoneyMaxFrame.builder()
                .streamId(UnsignedLong.ONE)
                .receiveMax(UnsignedLong.ONE)
                .totalReceived(UnsignedLong.valueOf(10L))
                .build()
        },
        // totalReceived=Max-1
        {
            StreamMoneyMaxFrame.builder()
                .streamId(UnsignedLong.ONE)
                .receiveMax(UnsignedLong.ONE)
                .totalReceived(UnsignedLong.MAX_VALUE.minus(UnsignedLong.ONE))
                .build()
        },
        // totalReceived=Max
        {
            StreamMoneyMaxFrame.builder()
                .streamId(UnsignedLong.ONE)
                .receiveMax(UnsignedLong.ONE)
                .totalReceived(UnsignedLong.MAX_VALUE)
                .build()
        },

    });
  }

}
