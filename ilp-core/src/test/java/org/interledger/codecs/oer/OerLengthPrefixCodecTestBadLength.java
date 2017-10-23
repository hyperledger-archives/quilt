package org.interledger.codecs.oer;

import org.interledger.codecs.CodecContext;
import org.interledger.codecs.oer.OerLengthPrefixCodec.OerLengthPrefix;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Tests that the {@link OerLengthPrefixCodec} correctly fails if the required length indicator
 * cannot be fully read.
 */
public class OerLengthPrefixCodecTestBadLength {

  @Test(expected = IOException.class)
  public void test_BadLengthIndicator() throws IOException {
    OerLengthPrefixCodec codec = new OerLengthPrefixCodec();
    CodecContext context = new CodecContext().register(OerLengthPrefix.class, codec);

    /*
     * we create an incorrect length indicator that says that the next two bytes encode the length
     * of the actual data. however, we'll only supply one byte of information. the codec should
     * detect this and fail, since it cannot read the length indicator.
     */
    byte lengthOfLength = (byte) ((1 << 7) | 2);

    byte[] lengthIndicator = new byte[] {lengthOfLength, 0};

    final ByteArrayInputStream inputStream = new ByteArrayInputStream(lengthIndicator);

    codec.read(context, inputStream);
  }

}
