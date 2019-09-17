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

 import org.interledger.core.InterledgerAddress;
 import org.interledger.stream.frames.ConnectionNewAddressFrame;

 import org.junit.runner.RunWith;
 import org.junit.runners.Parameterized;
 import org.junit.runners.Parameterized.Parameters;

 import java.util.Arrays;
 import java.util.Collection;

 /**
  * Unit tests for {@link AsnConnectionNewAddressFrameCodec}.
  */
 @RunWith(Parameterized.class)
 public class AsnConnectionNewAddressFrameCodecTest extends AbstractAsnFrameCodecTest<ConnectionNewAddressFrame> {

   private static final String JUST_RIGHT =
       "g.foo.0123"
           + "45678901234567890123456789012345678901234567890123456789012345678901234567890123456"
           + "78901234567890123456789012345678901234567890123456789012345678901234567890123456789"
           + "01234567890123456789012345678901234567890123456789012345678901234567890123456789012"
           + "34567890123456789012345678901234567890123456789012345678901234567890123456789012345"
           + "67890123456789012345678901234567890123456789012345678901234567890123456789012345678"
           + "90123456789012345678901234567890123456789012345678901234567890123456789012345678901"
           + "23456789012345678901234567890123456789012345678901234567890123456789012345678901234"
           + "56789012345678901234567890123456789012345678901234567890123456789012345678901234567"
           + "89012345678901234567890123456789012345678901234567890123456789012345678901234567890"
           + "12345678901234567890123456789012345678901234567890123456789012345678901234567890123"
           + "45678901234567890123456789012345678901234567890123456789012345678901234567890123456"
           + "78901234567890123456789012345678901234567890123456789012345678901234567890123456789"
           + "01234567891234567";

   public AsnConnectionNewAddressFrameCodecTest() {
     super(ConnectionNewAddressFrame.class);
   }

   /**
    * The data for this test...
    */
   @Parameters
   public static Collection<Object[]> data() {

     return Arrays.asList(new Object[][] {

         // short address
         {
             ConnectionNewAddressFrame.builder()
                 .sourceAddress(InterledgerAddress.of("test.foo"))
                 .build()
         },
         // long address
         {
             ConnectionNewAddressFrame.builder()
                 .sourceAddress(InterledgerAddress.of(JUST_RIGHT))
                 .build()
         },
     });
   }
 }
