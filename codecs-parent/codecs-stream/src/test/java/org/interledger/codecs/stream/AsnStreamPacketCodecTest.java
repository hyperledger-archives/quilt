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

 package org.interledger.codecs.stream;

 import org.interledger.core.InterledgerPacketType;
 import org.interledger.encoding.asn.framework.CodecContext;
 import org.interledger.stream.StreamPacket;
 import org.interledger.stream.frames.StreamMoneyFrame;

 import com.google.common.primitives.Bytes;
 import com.google.common.primitives.UnsignedLong;
 import org.junit.Test;
 import org.junit.runner.RunWith;
 import org.junit.runners.Parameterized;
 import org.junit.runners.Parameterized.Parameter;
 import org.junit.runners.Parameterized.Parameters;

 import java.io.ByteArrayInputStream;
 import java.io.ByteArrayOutputStream;
 import java.io.IOException;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.Random;

 import static org.assertj.core.api.Assertions.assertThat;

 /**
  * Unit tests for {@link AsnStreamPacketCodec}.
  */
 @RunWith(Parameterized.class)
 public class AsnStreamPacketCodecTest {

   // first data value (0) is default
   @Parameter
   public StreamPacket streamPacket;

   /**
    * The data for this test...
    */
   @Parameters
   public static Collection<Object[]> data() {

     return Arrays.asList(new Object[][] {

         // type = PREPARE
         {
             StreamPacket.builder()
                 .interledgerPacketType(InterledgerPacketType.PREPARE)
                 .prepareAmount(UnsignedLong.ONE)
                 .sequence(UnsignedLong.MAX_VALUE)
                 .addFrames(
                     StreamMoneyFrame.builder()
                         .streamId(UnsignedLong.ONE)
                         .shares(UnsignedLong.ONE)
                         .build()
                 )
                 .build()
         },
         // type = FULFILL
         {
             StreamPacket.builder()
                 .interledgerPacketType(InterledgerPacketType.FULFILL)
                 .prepareAmount(UnsignedLong.ONE)
                 .sequence(UnsignedLong.MAX_VALUE)
                 .addFrames(
                     StreamMoneyFrame.builder()
                         .streamId(UnsignedLong.ONE)
                         .shares(UnsignedLong.ONE)
                         .build()
                 )
                 .build()
         },
         // type = REJECT
         {
             StreamPacket.builder()
                 .interledgerPacketType(InterledgerPacketType.REJECT)
                 .prepareAmount(UnsignedLong.ONE)
                 .sequence(UnsignedLong.MAX_VALUE)
                 .addFrames(
                     StreamMoneyFrame.builder()
                         .streamId(UnsignedLong.ONE)
                         .shares(UnsignedLong.ONE)
                         .build()
                 )
                 .build()
         },
     });
   }

   @Test
   public void testWriteThenRead() throws IOException {
     final CodecContext context = StreamCodecContextFactory.oer();

     // Encode...
     final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
     context.write(this.streamPacket, byteArrayOutputStream);

     // Decode...
     ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
     final StreamPacket decodedStreamPacket = context.read(StreamPacket.class, byteArrayInputStream);

     assertThat(decodedStreamPacket).isEqualTo(this.streamPacket);
   }

   @Test
   public void tesJunkDataIgnored() throws IOException {
     final CodecContext context = StreamCodecContextFactory.oer();

     // Encode...
     final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
     context.write(this.streamPacket, byteArrayOutputStream);

     byte[] junkData = new byte[100];
     Random rand = new Random();
     rand.nextBytes(junkData);

     // Decode...
     ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
         Bytes.concat(byteArrayOutputStream.toByteArray(), junkData)
     );
     final StreamPacket decodedStreamPacket = context.read(StreamPacket.class, byteArrayInputStream);

     assertThat(decodedStreamPacket).isEqualTo(this.streamPacket);
   }

 }
