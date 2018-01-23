package org.interledger.codecs.framework;

import org.interledger.codecs.MyCustomObject;
import org.interledger.codecs.asn.AsnMyCustomObject;
import org.interledger.codecs.asn.AsnObject;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class EncodingTests {

  @Test
  public void test() throws Exception {

    CodecContext context = CodecContextFactory.getContext(CodecContextFactory.OCTET_ENCODING_RULES)
        .register(MyCustomObject.class, () -> new AsnMyCustomObject());

    MyCustomObject obj = new MyCustomObject();
    obj.Property1 = "Test";
    obj.Property2 = 123;

    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      context.write(obj, baos);
      byte[] bytes = baos.toByteArray();

      try(ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {

        MyCustomObject obj1 = (MyCustomObject) context.read(MyCustomObject.class, bais);

        System.out.print(obj1);
      }

    }


  }
}
