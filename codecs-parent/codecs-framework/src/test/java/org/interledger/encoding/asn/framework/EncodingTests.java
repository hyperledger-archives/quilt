package org.interledger.encoding.asn.framework;

/*-
 * ========================LICENSE_START=================================
 * Interledger Codec Framework
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

import org.interledger.encoding.MyCustomObject;
import org.interledger.encoding.asn.AsnMyCustomObjectCodec;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;

public class EncodingTests {

  @Test
  public void test1() throws Exception {

    CodecContext context = CodecContextFactory.oer()
        .register(MyCustomObject.class, () -> new AsnMyCustomObjectCodec());

    MyCustomObject obj = MyCustomObject.builder()
        .utf8StringProperty("Hello")
        .fixedLengthUtf8StringProperty("1234")
        .uint8Property((short) 255)
        .uint16Property(65535)
        .uint32Property(1234567L)
        .uint64Property(UnsignedLong.valueOf(BigInteger.probablePrime(64, new SecureRandom())))
        .octetStringProperty(new byte[0])
        .fixedLengthOctetStringProperty(
            new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4,
                5, 6, 7, 8, 9, 0, 1,})
        .uintProperty(BigInteger.TEN)
        .build();

    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      context.write(obj, baos);
      byte[] bytes = baos.toByteArray();

      try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {

        MyCustomObject obj1 = context.read(MyCustomObject.class, bais);

        System.out.print(obj1);
      }

    }

  }


  @Test
  public void test2() throws Exception {

    CodecContext context = CodecContextFactory.oer().register(MyCustomObject.class, AsnMyCustomObjectCodec::new);

    MyCustomObject obj = MyCustomObject.builder()
        .utf8StringProperty("World")
        .fixedLengthUtf8StringProperty("ABCD")
        .uint8Property((short) 1)
        .uint16Property(1024)
        .uint32Property(1234567L)
        .uint64Property(UnsignedLong.valueOf(BigInteger.probablePrime(64, new SecureRandom())))
        .octetStringProperty(new byte[] {0, 1, 2, 4})
        .fixedLengthOctetStringProperty(
            new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4,
                5, 6, 7, 8, 9, 0, 1,}
        )
        .uintProperty(BigInteger.ZERO)
        .build();

    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

      context.write(obj, baos);
      byte[] bytes = baos.toByteArray();
      try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
        MyCustomObject obj2 = context.read(MyCustomObject.class, bais);
        System.out.print(obj2);
      }
    }

  }

}
