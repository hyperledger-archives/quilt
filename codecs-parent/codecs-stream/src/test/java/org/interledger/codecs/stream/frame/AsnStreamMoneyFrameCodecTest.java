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

 import org.interledger.stream.frames.StreamMoneyFrame;

 import com.google.common.primitives.UnsignedLong;
 import org.junit.runner.RunWith;
 import org.junit.runners.Parameterized;
 import org.junit.runners.Parameterized.Parameters;

 import java.util.Arrays;
 import java.util.Collection;

 /**
  * Unit tests for {@link AsnStreamMoneyFrameCodec}.
  */
 @RunWith(Parameterized.class)
 public class AsnStreamMoneyFrameCodecTest extends AbstractAsnFrameCodecTest<StreamMoneyFrame> {

   public AsnStreamMoneyFrameCodecTest() {
     super(StreamMoneyFrame.class);
   }

   /**
    * The data for this test...
    */
   @Parameters
   public static Collection<Object[]> data() {

     return Arrays.asList(new Object[][] {
         // streamId=0
         {
             StreamMoneyFrame.builder()
                 .streamId(UnsignedLong.ZERO)
                 .shares(UnsignedLong.valueOf(10L))
                 .build()
         },
         // streamId=1
         {
             StreamMoneyFrame.builder()
                 .streamId(UnsignedLong.ONE)
                 .shares(UnsignedLong.valueOf(10L))
                 .build()
         },
         // streamId=10
         {
             StreamMoneyFrame.builder()
                 .streamId(UnsignedLong.valueOf(10L))
                 .shares(UnsignedLong.valueOf(10L))
                 .build()
         },
         // streamId=Max-1
         {
             StreamMoneyFrame.builder()
                 .streamId(UnsignedLong.MAX_VALUE.minus(UnsignedLong.ONE))
                 .shares(UnsignedLong.valueOf(10L))
                 .build()
         },
         // streamId=Max
         {
             StreamMoneyFrame.builder()
                 .streamId(UnsignedLong.MAX_VALUE)
                 .shares(UnsignedLong.valueOf(10L))
                 .build()
         },
         // Shares=0
         {
             StreamMoneyFrame.builder()
                 .streamId(UnsignedLong.ONE)
                 .shares(UnsignedLong.ZERO)
                 .build()
         },
         // Shares=1
         {
             StreamMoneyFrame.builder()
                 .streamId(UnsignedLong.ONE)
                 .shares(UnsignedLong.ONE)
                 .build()
         },
         // Shares=10
         {
             StreamMoneyFrame.builder()
                 .streamId(UnsignedLong.ONE)
                 .shares(UnsignedLong.valueOf(10L))
                 .build()
         },
         // Shares=Max-1
         {
             StreamMoneyFrame.builder()
                 .streamId(UnsignedLong.ONE)
                 .shares(UnsignedLong.MAX_VALUE.minus(UnsignedLong.ONE))
                 .build()
         },
         // Shares=Max
         {
             StreamMoneyFrame.builder()
                 .streamId(UnsignedLong.ONE)
                 .shares(UnsignedLong.MAX_VALUE)
                 .build()
         },

     });
   }

 }
