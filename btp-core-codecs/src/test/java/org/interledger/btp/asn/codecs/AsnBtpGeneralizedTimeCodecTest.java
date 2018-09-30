package org.interledger.btp.asn.codecs;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Before;
import org.junit.Test;

import java.time.Instant;

/**
 * Unit tests for {@link AsnBtpGeneralizedTimeCodec}.
 */
public class AsnBtpGeneralizedTimeCodecTest {

  private AsnBtpGeneralizedTimeCodec codec;

  @Before
  public void setup() {
    this.codec = new AsnBtpGeneralizedTimeCodec();
  }

  @Test
  public void encodeDecode() {
    final Instant initial = Instant.parse("2018-12-24T11:59:23Z");
    codec.encode(initial);
    assertThat(codec.getCharString(), is("20181224115923Z"));
    final Instant actual = codec.decode();
    assertThat(actual, is(initial));
  }
}