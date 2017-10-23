package org.interledger.codecs.oer;

import org.interledger.codecs.CodecContext;
import org.interledger.codecs.oer.OerIA5StringCodec.OerIA5String;
import org.interledger.codecs.oer.OerLengthPrefixCodec.OerLengthPrefix;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Test to ensure that the {@link OerIA5StringCodec} properly fails when it cannot read the number
 * of bytes indicated.
 */
public class OerIA5StringCodecTestBadLength {

  @Test(expected = IOException.class)
  public void test_BadLengthIndicator() throws IOException {
    OerIA5StringCodec codec = new OerIA5StringCodec();
    CodecContext context =
        new CodecContext().register(OerLengthPrefix.class, new OerLengthPrefixCodec())
            .register(OerIA5String.class, codec);

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    codec.write(context, new OerIA5String("A test string of reasonable length"), outputStream);

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
    codec.read(context, inputStream);
  }

}
