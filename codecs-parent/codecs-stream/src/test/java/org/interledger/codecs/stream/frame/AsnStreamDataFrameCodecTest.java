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

 import org.interledger.stream.crypto.Random;
 import org.interledger.stream.frames.StreamDataFrame;

 import com.google.common.primitives.UnsignedLong;
 import org.junit.runner.RunWith;
 import org.junit.runners.Parameterized;
 import org.junit.runners.Parameterized.Parameters;

 import java.util.Arrays;
 import java.util.Collection;

 /**
  * Unit tests for {@link AsnStreamDataFrameCodec}.
  */
 @RunWith(Parameterized.class)
 public class AsnStreamDataFrameCodecTest extends AbstractAsnFrameCodecTest<StreamDataFrame> {

   public AsnStreamDataFrameCodecTest() {
     super(StreamDataFrame.class);
   }

   /**
    * The data for this test...
    */
   @Parameters
   public static Collection<Object[]> data() {

     return Arrays.asList(new Object[][] {
         // streamId=0
         {
             StreamDataFrame.builder()
                 .streamId(UnsignedLong.ZERO)
                 .offset(UnsignedLong.valueOf(10L))
                 .build()
         },
         // streamId=1
         {
             StreamDataFrame.builder()
                 .streamId(UnsignedLong.ONE)
                 .offset(UnsignedLong.valueOf(10L))
                 .build()
         },
         // streamId=10
         {
             StreamDataFrame.builder()
                 .streamId(UnsignedLong.valueOf(10L))
                 .offset(UnsignedLong.valueOf(10L))
                 .build()
         },
         // streamId=Max-1
         {
             StreamDataFrame.builder()
                 .streamId(UnsignedLong.MAX_VALUE.minus(UnsignedLong.ONE))
                 .offset(UnsignedLong.valueOf(10L))
                 .build()
         },
         // streamId=Max
         {
             StreamDataFrame.builder()
                 .streamId(UnsignedLong.MAX_VALUE)
                 .offset(UnsignedLong.valueOf(10L))
                 .build()
         },
         // Offset=0
         {
             StreamDataFrame.builder()
                 .streamId(UnsignedLong.ONE)
                 .offset(UnsignedLong.ZERO)
                 .build()
         },
         // Offset=1
         {
             StreamDataFrame.builder()
                 .streamId(UnsignedLong.ONE)
                 .offset(UnsignedLong.ONE)
                 .build()
         },
         // Offset=10
         {
             StreamDataFrame.builder()
                 .streamId(UnsignedLong.ONE)
                 .offset(UnsignedLong.valueOf(10L))
                 .build()
         },
         // Offset=Max-1
         {
             StreamDataFrame.builder()
                 .streamId(UnsignedLong.ONE)
                 .offset(UnsignedLong.MAX_VALUE.minus(UnsignedLong.ONE))
                 .build()
         },
         // Offset=Max
         {
             StreamDataFrame.builder()
                 .streamId(UnsignedLong.ONE)
                 .offset(UnsignedLong.MAX_VALUE)
                 .build()
         },

         // small data
         {
             StreamDataFrame.builder()
                 .streamId(UnsignedLong.ONE)
                 .offset(UnsignedLong.MAX_VALUE)
                 .data((byte) 1)
                 .build()
         },

         // bigger data
         {
             StreamDataFrame.builder()
                 .streamId(UnsignedLong.ONE)
                 .offset(UnsignedLong.MAX_VALUE)
                 .data(Random.randBytes(512))
                 .build()
         },
     });
   }

 }
