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

 import static org.hamcrest.MatcherAssert.assertThat;
 import static org.hamcrest.core.Is.is;

 import org.interledger.encoding.asn.framework.CodecContext;
 import org.interledger.stream.AmountTooLargeErrorData;

 import com.google.common.primitives.UnsignedLong;
 import org.hamcrest.CoreMatchers;
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

 /**
  * Unit tests for {@link AsnAmountTooLargeDataCodec}.
  */
 @RunWith(Parameterized.class)
 public class AsnAmountTooLargeDataCodecTest {

   // first data value (0) is default
   @Parameter
   public AmountTooLargeErrorData errorData;

   /**
    * The data for this test...
    */
   @Parameters
   public static Collection<Object[]> data() {

     return Arrays.asList(new Object[][] {

         // receivedAmount = 0
         {
             AmountTooLargeErrorData.builder()
                 .receivedAmount(UnsignedLong.ZERO)
                 .maximumAmount(UnsignedLong.ONE)
                 .build()
         },
         // receivedAmount = 1
         {
             AmountTooLargeErrorData.builder()
                 .receivedAmount(UnsignedLong.ONE)
                 .maximumAmount(UnsignedLong.ONE)
                 .build()
         },
         // receivedAmount = 10
         {
             AmountTooLargeErrorData.builder()
                 .receivedAmount(UnsignedLong.valueOf(10L))
                 .maximumAmount(UnsignedLong.ONE)
                 .build()
         },
         // receivedAmount = Long.Max - 1
         {
             AmountTooLargeErrorData.builder()
                 .receivedAmount(UnsignedLong.MAX_VALUE.minus(UnsignedLong.ONE))
                 .maximumAmount(UnsignedLong.ONE)
                 .build()
         },
         // receivedAmount = Long.Max
         {
             AmountTooLargeErrorData.builder()
                 .receivedAmount(UnsignedLong.MAX_VALUE)
                 .maximumAmount(UnsignedLong.ONE)
                 .build()
         },
         // maximumAmount = 0
         {
             AmountTooLargeErrorData.builder()
                 .receivedAmount(UnsignedLong.ONE)
                 .maximumAmount(UnsignedLong.ZERO)
                 .build()
         },
         // maximumAmount = 1
         {
             AmountTooLargeErrorData.builder()
                 .receivedAmount(UnsignedLong.ONE)
                 .maximumAmount(UnsignedLong.ONE)
                 .build()
         },
         // maximumAmount = 10
         {
             AmountTooLargeErrorData.builder()
                 .receivedAmount(UnsignedLong.ONE)
                 .maximumAmount(UnsignedLong.valueOf(10L))
                 .build()
         },
         // maximumAmount = Long.Max - 1
         {
             AmountTooLargeErrorData.builder()
                 .receivedAmount(UnsignedLong.ONE)
                 .maximumAmount(UnsignedLong.MAX_VALUE.minus(UnsignedLong.ONE))
                 .build()
         },
         // maximumAmount = Long.Max
         {
             AmountTooLargeErrorData.builder()
                 .receivedAmount(UnsignedLong.ONE)
                 .maximumAmount(UnsignedLong.MAX_VALUE)
                 .build()
         },

     });
   }

   /**
    * The primary difference between this test and {@link #testInterledgerPaymentCodec()} is that this context call
    * specifies the type, whereas the test below determines the type from the payload.
    */
   @Test
   public void testIndividualRead() throws IOException {
     final CodecContext context = StreamCodecContextFactory.oer();
     final ByteArrayInputStream asn1OererrorDataBytes = constructerrorDataBytes();

     final AmountTooLargeErrorData errorData = context.read(AmountTooLargeErrorData.class, asn1OererrorDataBytes);
     assertThat(errorData, is(errorData));
   }

   /**
    * The primary difference between this test and {@link #testIndividualRead()} is that this context determines the ipr
    * type from the payload, whereas the test above specifies the type in the method call.
    */
   @Test
   public void testInterledgerPaymentCodec() throws Exception {
     final CodecContext context = StreamCodecContextFactory.oer();
     final ByteArrayInputStream asn1OererrorDataBytes = constructerrorDataBytes();

     final AmountTooLargeErrorData decodederrorData = context
         .read(AmountTooLargeErrorData.class, asn1OererrorDataBytes);
     assertThat(decodederrorData.getClass().getName(), CoreMatchers.is(errorData.getClass().getName()));
     assertThat(decodederrorData, CoreMatchers.is(errorData));
   }

   protected ByteArrayInputStream constructerrorDataBytes() throws IOException {
     final CodecContext context = StreamCodecContextFactory.oer();

     final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
     context.write(errorData, outputStream);

     return new ByteArrayInputStream(outputStream.toByteArray());
   }

 }
