package org.interledger.codecs.oer;

import org.interledger.codecs.asn.AsnIA5String;
import org.interledger.codecs.asn.AsnSizeConstraint;
import org.interledger.codecs.framework.CodecContext;
import org.interledger.codecs.framework.CodecContextFactory;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Test to ensure that the {@link AsnCharStringOerSerializer} properly fails when it cannot read
 * the number of bytes indicated.
 */
public class IA5StringOerSerializerTestBadLength {

  @Test(expected = IOException.class)
  public void test_BadLengthIndicator() throws IOException {

    CodecContext context = CodecContextFactory.getContext(CodecContextFactory.OCTET_ENCODING_RULES)
        .register(String.class, () -> new AsnIA5String(AsnSizeConstraint.unconstrained()));

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    context.write("A test string of reasonable length", outputStream);

    final byte[] correctData = outputStream.toByteArray();

    /*
     * we now remove a portion of the data, i.e. the length indicator should claim that there are 34
     * bytes, but we have truncated to 10 bytes
     */

    final byte[] truncated = Arrays.copyOfRange(correctData, 0, 10);

    /* create an input stream over the truncated data */
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(truncated);

    /*
     * try read the data, the codec should fail since it reaches EOF before consuming the 34 bytes
     * indicated.
     */
    context.read(String.class, inputStream);
  }

}
