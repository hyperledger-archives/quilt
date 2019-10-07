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

 import org.interledger.codecs.stream.StreamCodecContextFactory;
 import org.interledger.encoding.asn.framework.CodecContext;
 import org.interledger.stream.frames.ConnectionDataMaxFrame;

 import com.google.common.primitives.UnsignedLong;
 import org.junit.Test;

 import java.io.ByteArrayInputStream;
 import java.io.ByteArrayOutputStream;
 import java.io.IOException;
 import java.text.NumberFormat;
 import java.util.Locale;

 /**
  * Unit test to see how long it takes to serialize/deserialize frames when the codec use a UnsignedLong vs an
  * UnsignedLong vs a Long.
  */
 public class FrameCodecPerfTest {

   /**
    * This test measures the avg amount of time to serialize and deserialize 1M frames.
    *
    * <pre>
    * Results:
    * =======
    * long: 1M Frames --> 0.9s (1,111,111 fps)
    * long: 10M Frames --> 6.2s (1,612,903 fps)
    *
    * UnsignedLong: 1M Frames --> 0.9s (1,111,111 fps)
    * UnsignedLong: 10M Frames --> 6.4s (1,562,500 fps)
    * UnsignedLong: 100M Frames --> 52s (1,922,485 fps)
    *
    * BigInteger: 1M Frames --> 1.06s (943,396 fps)
    * BigInteger: 10M Frames --> 6.9s (1,449,275 fps)
    * </pre>
    */
   @Test
   public void testPerf() throws IOException {
     final CodecContext context = StreamCodecContextFactory.oer();
     final ConnectionDataMaxFrame frame = ConnectionDataMaxFrame.builder()
         .maxOffset(UnsignedLong.ZERO)
         //.maxOffset(BigInteger.ZERO)
         //.maxOffset(0)
         .build();
     final int numFrames = 1000;

     // Warmup
     for (int i = 0; i < 50; i++) {
       // Serialize
       final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
       context.write(frame, outputStream);
       final ByteArrayInputStream asn1OerFrameBytes = new ByteArrayInputStream(outputStream.toByteArray());

       // DeSerialize
       context.read(ConnectionDataMaxFrame.class, asn1OerFrameBytes);
     }

     final long start = System.nanoTime();
     final long startMs = System.currentTimeMillis();
     for (int i = 0; i < numFrames; i++) {
       // Serialize
       final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
       context.write(frame, outputStream);
       final ByteArrayInputStream asn1OerFrameBytes = new ByteArrayInputStream(outputStream.toByteArray());

       // DeSerialize
       context.read(ConnectionDataMaxFrame.class, asn1OerFrameBytes);
     }

     final long end = System.nanoTime();
     final long endMs = System.currentTimeMillis();
     final long duration = (end - start);
     final long durationMs = (endMs - startMs);

     System.out.println(
         String.format("Serialize+Deserialize %s frames took %s ns (%s ms)",
             NumberFormat.getNumberInstance(Locale.US).format(numFrames),
             NumberFormat.getNumberInstance(Locale.US).format(duration),
             NumberFormat.getNumberInstance(Locale.US).format(durationMs)
         ));
   }

 }
