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

 import static org.hamcrest.CoreMatchers.is;
 import static org.hamcrest.MatcherAssert.assertThat;

 import org.interledger.codecs.stream.StreamCodecContextFactory;
 import org.interledger.encoding.asn.framework.CodecContext;
 import org.interledger.stream.frames.ConnectionNewAddressFrame;
 import org.interledger.stream.frames.StreamFrame;

 import org.junit.Test;
 import org.junit.runners.Parameterized.Parameter;

 import java.io.ByteArrayInputStream;
 import java.io.ByteArrayOutputStream;
 import java.io.IOException;

 /**
  * Abstract helper class for all StreamFrame Codec tests.
  */
 public abstract class AbstractAsnFrameCodecTest<T extends StreamFrame> {

   // first data value (0) is default
   @Parameter
   public T frame;

   /**
    * The primary difference between this test and {@link #testInterledgerPaymentCodec()} is that this context call
    * specifies the type, whereas the test below determines the type from the payload.
    */
   public abstract void testIndividualRead() throws IOException;

   /**
    * The primary difference between this test and {@link #testIndividualRead()} is that this context determines the ipr
    * type from the payload, whereas the test above specifies the type in the method call.
    */
   @Test
   public void testInterledgerPaymentCodec() throws Exception {
     final CodecContext context = StreamCodecContextFactory.oer();
     final ByteArrayInputStream asn1OerFrameBytes = constructFrameBytes();

     final StreamFrame decodedFrame = context.read(StreamFrame.class, asn1OerFrameBytes);
     assertThat(decodedFrame.getClass().getName(), is(frame.getClass().getName()));
     assertThat(decodedFrame, is(frame));
   }

   protected ByteArrayInputStream constructFrameBytes() throws IOException {
     final CodecContext context = StreamCodecContextFactory.oer();

     final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
     context.write(frame, outputStream);

     return new ByteArrayInputStream(outputStream.toByteArray());
   }

 }
