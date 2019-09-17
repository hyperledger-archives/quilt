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

 import org.interledger.stream.frames.ConnectionAssetDetailsFrame;

 import org.junit.runner.RunWith;
 import org.junit.runners.Parameterized;
 import org.junit.runners.Parameterized.Parameters;

 import java.util.Arrays;
 import java.util.Collection;

 /**
  * Unit tests for {@link AsnConnectionAssetDetailsFrameCodec}.
  */
 @RunWith(Parameterized.class)
 public class AsnConnectionAssetDetailsFrameCodecTest extends AbstractAsnFrameCodecTest<ConnectionAssetDetailsFrame> {

   public AsnConnectionAssetDetailsFrameCodecTest() {
     super(ConnectionAssetDetailsFrame.class);
   }

   /**
    * The data for this test...
    */
   @Parameters
   public static Collection<Object[]> data() {

     return Arrays.asList(new Object[][] {
         // assetScale=0
         {
             ConnectionAssetDetailsFrame.builder()
                 .sourceAssetCode("USD")
                 .sourceAssetScale((short) 0)
                 .build()
         },
         // assetScale=1
         {
             ConnectionAssetDetailsFrame.builder()
                 .sourceAssetCode("USD")
                 .sourceAssetScale((short) 1)
                 .build()
         },
         // assetScale=10
         {
             ConnectionAssetDetailsFrame.builder()
                 .sourceAssetCode("USD")
                 .sourceAssetScale((short) 10)
                 .build()
         },
         // assetScale=Max-1
         {
             ConnectionAssetDetailsFrame.builder()
                 .sourceAssetCode("USD")
                 .sourceAssetScale((short) (254))
                 .build()
         },
         // assetScale=Max
         {
             ConnectionAssetDetailsFrame.builder()
                 .sourceAssetCode("USD")
                 .sourceAssetScale((short) 255)
                 .build()
         },

         // empty code
         {
             ConnectionAssetDetailsFrame.builder()
                 .sourceAssetCode("")
                 .sourceAssetScale((short) 255)
                 .build()
         },
         // ShortCode=0
         {
             ConnectionAssetDetailsFrame.builder()
                 .sourceAssetCode("U")
                 .sourceAssetScale((short) 255)
                 .build()
         },
         // MediumCode
         {
             ConnectionAssetDetailsFrame.builder()
                 .sourceAssetCode("US")
                 .sourceAssetScale((short) 255)
                 .build()
         },
         {
             ConnectionAssetDetailsFrame.builder()
                 .sourceAssetCode("USD")
                 .sourceAssetScale((short) 255)
                 .build()
         },
         // LongCode
         {
             ConnectionAssetDetailsFrame.builder()
                 .sourceAssetCode("DASH")
                 .sourceAssetScale((short) 255)
                 .build()
         },
         // LongerCode
         {
             ConnectionAssetDetailsFrame.builder()
                 .sourceAssetCode("DASH-DASH-DASH")
                 .sourceAssetScale((short) 255)
                 .build()
         },
         // Chinese String (UTF)
         {
             ConnectionAssetDetailsFrame.builder()
                 .sourceAssetCode("元元元")
                 .sourceAssetScale((short) 255)
                 .build()
         },
         // Russian String (UTF)
         {
             ConnectionAssetDetailsFrame.builder()
                 .sourceAssetCode("₽")
                 .sourceAssetScale((short) 255)
                 .build()
         },
     });
   }

 }
