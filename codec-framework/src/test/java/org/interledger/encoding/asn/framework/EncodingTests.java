package org.interledger.encoding.asn.framework;

import org.interledger.encoding.MyCustomObject;
import org.interledger.encoding.asn.AsnMyCustomObjectCodec;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;

public class EncodingTests {

  @Test
  public void test1() throws Exception {

    CodecContext context = CodecContextFactory.getContext(CodecContextFactory.OCTET_ENCODING_RULES)
        .register(MyCustomObject.class, () -> new AsnMyCustomObjectCodec());

    MyCustomObject obj = MyCustomObject.builder()
        .utf8StringProperty("Hello")
        .fixedLengthUtf8StringProperty("1234")
        .uint8Property(255)
        .uint32Property(1234567L)
        .uint64Property(BigInteger.probablePrime(64, new SecureRandom()))
        .octetStringProperty(new byte[]{})
        .fixedLengthOctetStringProperty(
            new byte[] {0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,})
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

    CodecContext context = CodecContextFactory.getContext(CodecContextFactory.OCTET_ENCODING_RULES)
        .register(MyCustomObject.class, () -> new AsnMyCustomObjectCodec());

    MyCustomObject obj = MyCustomObject.builder()
        .utf8StringProperty("World")
        .fixedLengthUtf8StringProperty("ABCD")
        .uint8Property(1)
        .uint32Property(1234567L)
        .uint64Property(BigInteger.probablePrime(64, new SecureRandom()))
        .octetStringProperty(new byte[]{0,1,2,4})
        .fixedLengthOctetStringProperty(
            new byte[] {0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,})
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
