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

 import static org.hamcrest.MatcherAssert.assertThat;
 import static org.hamcrest.core.Is.is;

 import org.interledger.codecs.stream.StreamCodecContextFactory;
 import org.interledger.encoding.asn.framework.CodecContext;
 import org.interledger.stream.frames.StreamDataBlockedFrame;

 import com.google.common.primitives.UnsignedLong;
 import org.junit.Test;
 import org.junit.runner.RunWith;
 import org.junit.runners.Parameterized;
 import org.junit.runners.Parameterized.Parameters;

 import java.io.ByteArrayInputStream;
 import java.io.IOException;
 import java.util.Arrays;
 import java.util.Collection;

 /**
  * Unit tests for {@link AsnStreamDataBlockedFrameCodec}.
  */
 @RunWith(Parameterized.class)
 public class AsnStreamDataBlockedFrameCodecTest extends AbstractAsnFrameCodecTest<StreamDataBlockedFrame> {

   /**
    * The data for this test...
    */
   @Parameters
   public static Collection<Object[]> data() {

     return Arrays.asList(new Object[][] {
         // streamId=0
         {
             StreamDataBlockedFrame.builder()
                 .streamId(UnsignedLong.ZERO)
                 .maxOffset(UnsignedLong.valueOf(10L))
                 .build()
         },
         // streamId=1
         {
             StreamDataBlockedFrame.builder()
                 .streamId(UnsignedLong.ONE)
                 .maxOffset(UnsignedLong.valueOf(10L))
                 .build()
         },
         // streamId=10
         {
             StreamDataBlockedFrame.builder()
                 .streamId(UnsignedLong.valueOf(10L))
                 .maxOffset(UnsignedLong.valueOf(10L))
                 .build()
         },
         // streamId=Max-1
         {
             StreamDataBlockedFrame.builder()
                 .streamId(UnsignedLong.MAX_VALUE.minus(UnsignedLong.ONE))
                 .maxOffset(UnsignedLong.valueOf(10L))
                 .build()
         },
         // streamId=Max
         {
             StreamDataBlockedFrame.builder()
                 .streamId(UnsignedLong.MAX_VALUE)
                 .maxOffset(UnsignedLong.valueOf(10L))
                 .build()
         },
         // MaxOffset=0
         {
             StreamDataBlockedFrame.builder()
                 .streamId(UnsignedLong.ONE)
                 .maxOffset(UnsignedLong.ZERO)
                 .build()
         },
         // MaxOffset=1
         {
             StreamDataBlockedFrame.builder()
                 .streamId(UnsignedLong.ONE)
                 .maxOffset(UnsignedLong.ONE)
                 .build()
         },
         // MaxOffset=10
         {
             StreamDataBlockedFrame.builder()
                 .streamId(UnsignedLong.ONE)
                 .maxOffset(UnsignedLong.valueOf(10L))
                 .build()
         },
         // MaxOffset=Max-1
         {
             StreamDataBlockedFrame.builder()
                 .streamId(UnsignedLong.ONE)
                 .maxOffset(UnsignedLong.MAX_VALUE.minus(UnsignedLong.ONE))
                 .build()
         },
         // MaxOffset=Max
         {
             StreamDataBlockedFrame.builder()
                 .streamId(UnsignedLong.ONE)
                 .maxOffset(UnsignedLong.MAX_VALUE)
                 .build()
         },

     });
   }

   /**
    * The primary difference between this test and {@link #testInterledgerPaymentCodec()} is that this context call
    * specifies the type, whereas the test below determines the type from the payload.
    */
   @Test
   @Override
   public void testIndividualRead() throws IOException {
     final CodecContext context = StreamCodecContextFactory.oer();
     final ByteArrayInputStream asn1OerFrameBytes = constructFrameBytes();

     final StreamDataBlockedFrame frame = context.read(StreamDataBlockedFrame.class, asn1OerFrameBytes);
     assertThat(frame, is(frame));
   }

 }
