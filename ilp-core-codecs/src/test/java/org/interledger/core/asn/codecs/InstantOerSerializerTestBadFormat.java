package org.interledger.core.asn.codecs;

import org.interledger.core.asn.framework.InterledgerCodecContextFactory;
import org.interledger.encoding.asn.framework.CodecContext;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;

/**
 * Test cases specifically dealing with reading badly formatted {@link AsnTimestampCodec}
 * instances.
 */
public class InstantOerSerializerTestBadFormat {

  @Test(expected = IOException.class)
  public void test_NoTime() throws IOException {
    final CodecContext context = InterledgerCodecContextFactory.oer();

    final byte[] encoded = InstantOerSerializerTest.encodeString("20170101");
    final ByteArrayInputStream bis = new ByteArrayInputStream(encoded);
    context.read(Instant.class, bis);
  }

  @Test(expected = IOException.class)
  public void test_NoMinutes() throws IOException {
    final CodecContext context = InterledgerCodecContextFactory.oer();

    final byte[] encoded = InstantOerSerializerTest.encodeString("2017010112");
    final ByteArrayInputStream bis = new ByteArrayInputStream(encoded);
    context.read(Instant.class, bis);
  }

  @Test(expected = IOException.class)
  public void test_NoSeconds() throws IOException {
    final CodecContext context = InterledgerCodecContextFactory.oer();

    final byte[] encoded = InstantOerSerializerTest.encodeString("201701011213");
    final ByteArrayInputStream bis = new ByteArrayInputStream(encoded);
    context.read(Instant.class, bis);
  }

  @Test(expected = IOException.class)
  public void test_NoMillis() throws IOException {
    final CodecContext context = InterledgerCodecContextFactory.oer();

    final byte[] encoded = InstantOerSerializerTest.encodeString("20170101121314");
    final ByteArrayInputStream bis = new ByteArrayInputStream(encoded);
    context.read(Instant.class, bis);
  }

  @Test(expected = IOException.class)
  public void test_MillisShort() throws IOException {
    final CodecContext context = InterledgerCodecContextFactory.oer();

    final byte[] encoded = InstantOerSerializerTest.encodeString("2017010112131401");
    final ByteArrayInputStream bis = new ByteArrayInputStream(encoded);
    context.read(Instant.class, bis);
  }

}
