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

 import org.interledger.stream.frames.ConnectionDataBlockedFrame;

 import com.google.common.primitives.UnsignedLong;
 import org.junit.runner.RunWith;
 import org.junit.runners.Parameterized;
 import org.junit.runners.Parameterized.Parameters;

 import java.util.Arrays;
 import java.util.Collection;

 /**
  * Unit tests for {@link AsnConnectionDataBlockedFrameCodec}.
  */
 @RunWith(Parameterized.class)
 public class AsnConnectionDataBlockedFrameCodecTest extends AbstractAsnFrameCodecTest<ConnectionDataBlockedFrame> {

   public AsnConnectionDataBlockedFrameCodecTest() {
     super(ConnectionDataBlockedFrame.class);
   }

   /**
    * The data for this test...
    */
   @Parameters
   public static Collection<Object[]> data() {

     return Arrays.asList(new Object[][] {
         // maxOffset = 0
         {
             ConnectionDataBlockedFrame.builder()
                 .maxOffset(UnsignedLong.ZERO)
                 .build()
         },
         // maxOffset = 1
         {
             ConnectionDataBlockedFrame.builder()
                 .maxOffset(UnsignedLong.ONE)
                 .build()
         },
         // maxOffset = 10
         {
             ConnectionDataBlockedFrame.builder()
                 .maxOffset(UnsignedLong.valueOf(10L))
                 .build()
         },
         // maxOffset = Long.Max - 1
         {
             ConnectionDataBlockedFrame.builder()
                 .maxOffset(UnsignedLong.MAX_VALUE.minus(UnsignedLong.ONE))
                 .build()
         },
         // maxOffset = Long.Max
         {
             ConnectionDataBlockedFrame.builder()
                 .maxOffset(UnsignedLong.MAX_VALUE)
                 .build()
         },
     });
   }
 }
