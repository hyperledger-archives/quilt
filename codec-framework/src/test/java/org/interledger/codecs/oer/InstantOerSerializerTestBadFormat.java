package org.interledger.codecs.oer;

import org.interledger.codecs.asn.AsnGeneralizedTime;
import org.interledger.codecs.framework.CodecContext;
import org.interledger.codecs.framework.CodecContextFactory;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;

/**
 * Test cases specifically dealing with reading badly formatted {@link AsnGeneralizedTime}
 * instances.
 */
public class InstantOerSerializerTestBadFormat {

  @Test(expected = IOException.class)
  public void test_NoTime() throws IOException {
    final CodecContext context = CodecContextFactory.getContext(
        CodecContextFactory.OCTET_ENCODING_RULES);

    final byte[] encoded = InstantOerSerializerTest.encodeString("20170101");
    final ByteArrayInputStream bis = new ByteArrayInputStream(encoded);
    context.read(Instant.class, bis);
  }

  @Test(expected = IOException.class)
  public void test_NoMinutes() throws IOException {
    final CodecContext context = CodecContextFactory.getContext(
        CodecContextFactory.OCTET_ENCODING_RULES);

    final byte[] encoded = InstantOerSerializerTest.encodeString("2017010112Z");
    final ByteArrayInputStream bis = new ByteArrayInputStream(encoded);
    context.read(Instant.class, bis);
  }

  @Test(expected = IOException.class)
  public void test_NoSeconds() throws IOException {
    final CodecContext context = CodecContextFactory.getContext(
        CodecContextFactory.OCTET_ENCODING_RULES);

    final byte[] encoded = InstantOerSerializerTest.encodeString("201701011213Z");
    final ByteArrayInputStream bis = new ByteArrayInputStream(encoded);
    context.read(Instant.class, bis);
  }

  @Test(expected = IOException.class)
  public void test_NoMillis() throws IOException {
    final CodecContext context = CodecContextFactory.getContext(
        CodecContextFactory.OCTET_ENCODING_RULES);

    final byte[] encoded = InstantOerSerializerTest.encodeString("20170101121314Z");
    final ByteArrayInputStream bis = new ByteArrayInputStream(encoded);
    context.read(Instant.class, bis);
  }

  @Test(expected = IllegalArgumentException.class)
  public void test_MillisShort() throws IOException {
    final CodecContext context = CodecContextFactory.getContext(
        CodecContextFactory.OCTET_ENCODING_RULES);

    final byte[] encoded = InstantOerSerializerTest.encodeString("20170101121314.01Z");
    final ByteArrayInputStream bis = new ByteArrayInputStream(encoded);
    context.read(Instant.class, bis);
  }

  @Test(expected = IllegalArgumentException.class)
  public void test_MillisLong() throws IOException {
    final CodecContext context = CodecContextFactory.getContext(
        CodecContextFactory.OCTET_ENCODING_RULES);

    final byte[] encoded = InstantOerSerializerTest.encodeString("20170101121314.0123Z");
    final ByteArrayInputStream bis = new ByteArrayInputStream(encoded);
    context.read(Instant.class, bis);
  }

  @Test(expected = IllegalArgumentException.class)
  public void test_NoZone() throws IOException {
    final CodecContext context = CodecContextFactory.getContext(
        CodecContextFactory.OCTET_ENCODING_RULES);

    final byte[] encoded = InstantOerSerializerTest.encodeString("20170101121314.012");
    final ByteArrayInputStream bis = new ByteArrayInputStream(encoded);
    context.read(Instant.class, bis);
  }
}
