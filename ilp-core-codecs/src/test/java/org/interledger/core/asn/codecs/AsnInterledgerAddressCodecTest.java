package org.interledger.core.asn.codecs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.interledger.core.InterledgerAddress;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link AsnInterledgerAddressCodec}.
 */
public class AsnInterledgerAddressCodecTest {

  private static final String G_FOO = "g.foo";
  private AsnInterledgerAddressCodec codec;

  @Before
  public void setUp() {
    codec = new AsnInterledgerAddressCodec();
    codec.setCharString(G_FOO);
  }

  @Test
  public void decode() {
    assertThat(codec.decode(), is(InterledgerAddress.of(G_FOO)));
  }

  @Test
  public void encode() {
    codec.encode(InterledgerAddress.of(G_FOO));
    assertThat(codec.getCharString(), is(G_FOO));
  }
}