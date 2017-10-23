package org.interledger.codecs.oer;

import org.interledger.codecs.CodecContext;
import org.interledger.codecs.CodecContextFactory;
import org.interledger.codecs.oer.OerGeneralizedTimeCodec.OerGeneralizedTime;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Test cases specifically dealing with reading badly formatted {@link OerGeneralizedTime}
 * instances.
 */
public class OerGeneralizeTimeCodecTestBadFormat {

  @Test(expected = IllegalArgumentException.class)
  public void test_NoTime() throws IOException {
    final CodecContext context = CodecContextFactory.interledger();

    final byte[] encoded = OerGeneralizedTimeCodecTest.encodeString("20170101");
    final ByteArrayInputStream bis = new ByteArrayInputStream(encoded);
    context.read(OerGeneralizedTime.class, bis);
  }

  @Test(expected = IllegalArgumentException.class)
  public void test_NoMinutes() throws IOException {
    final CodecContext context = CodecContextFactory.interledger();

    final byte[] encoded = OerGeneralizedTimeCodecTest.encodeString("2017010112Z");
    final ByteArrayInputStream bis = new ByteArrayInputStream(encoded);
    context.read(OerGeneralizedTime.class, bis);
  }

  @Test(expected = IllegalArgumentException.class)
  public void test_NoSeconds() throws IOException {
    final CodecContext context = CodecContextFactory.interledger();

    final byte[] encoded = OerGeneralizedTimeCodecTest.encodeString("201701011213Z");
    final ByteArrayInputStream bis = new ByteArrayInputStream(encoded);
    context.read(OerGeneralizedTime.class, bis);
  }

  @Test(expected = IllegalArgumentException.class)
  public void test_NoMillis() throws IOException {
    final CodecContext context = CodecContextFactory.interledger();

    final byte[] encoded = OerGeneralizedTimeCodecTest.encodeString("20170101121314Z");
    final ByteArrayInputStream bis = new ByteArrayInputStream(encoded);
    context.read(OerGeneralizedTime.class, bis);
  }

  @Test(expected = IllegalArgumentException.class)
  public void test_MillisShort() throws IOException {
    final CodecContext context = CodecContextFactory.interledger();

    final byte[] encoded = OerGeneralizedTimeCodecTest.encodeString("20170101121314.01Z");
    final ByteArrayInputStream bis = new ByteArrayInputStream(encoded);
    context.read(OerGeneralizedTime.class, bis);
  }

  @Test(expected = IllegalArgumentException.class)
  public void test_MillisLong() throws IOException {
    final CodecContext context = CodecContextFactory.interledger();

    final byte[] encoded = OerGeneralizedTimeCodecTest.encodeString("20170101121314.0123Z");
    final ByteArrayInputStream bis = new ByteArrayInputStream(encoded);
    context.read(OerGeneralizedTime.class, bis);
  }

  @Test(expected = IllegalArgumentException.class)
  public void test_NoZone() throws IOException {
    final CodecContext context = CodecContextFactory.interledger();

    final byte[] encoded = OerGeneralizedTimeCodecTest.encodeString("20170101121314.012");
    final ByteArrayInputStream bis = new ByteArrayInputStream(encoded);
    context.read(OerGeneralizedTime.class, bis);
  }
}
