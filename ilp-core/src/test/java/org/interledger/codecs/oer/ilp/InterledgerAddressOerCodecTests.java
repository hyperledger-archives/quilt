package org.interledger.codecs.oer.ilp;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.interledger.InterledgerAddress;
import org.interledger.codecs.CodecContext;
import org.interledger.codecs.CodecContextFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class InterledgerAddressOerCodecTests {

  private static final String JUST_RIGHT =
      "g.012345678901234567890123456789012345678901234567890123456789012345678901234567890123456"
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
          + "012345678912312349393";

  // first data value (0) is default
  @Parameter
  public InterledgerAddress interledgerAddress;

  /**
   * The data for this test...
   */
  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{{InterledgerAddress.builder().value("test1.").build()},
        {InterledgerAddress.builder().value("test3.foo").build()},
        {InterledgerAddress.builder().value("test2.foo").build()},
        {InterledgerAddress.builder().value("test1.foo").build()},
        {InterledgerAddress.builder().value("test1.foo.bar.baz").build()},
        {InterledgerAddress.builder().value("private.secret.account").build()},
        {InterledgerAddress.builder().value("example.foo").build()},
        {InterledgerAddress.builder().value("peer.foo").build()},
        {InterledgerAddress.builder().value("self.foo").build()},
        {InterledgerAddress.builder().value(JUST_RIGHT).build()},});
  }

  @Test
  public void testEqualsHashcode() throws Exception {
    final CodecContext context = CodecContextFactory.interledger();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    context.write(interledgerAddress, outputStream);

    final ByteArrayInputStream byteArrayInputStream =
        new ByteArrayInputStream(outputStream.toByteArray());

    final InterledgerAddress decodedInterledgerAddress =
        context.read(InterledgerAddress.class, byteArrayInputStream);
    assertThat(decodedInterledgerAddress.getClass().getName(),
        is(interledgerAddress.getClass().getName()));
    assertThat(decodedInterledgerAddress, is(interledgerAddress));
  }
}
