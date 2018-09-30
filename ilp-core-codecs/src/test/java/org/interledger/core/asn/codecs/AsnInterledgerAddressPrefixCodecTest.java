package org.interledger.core.asn.codecs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.interledger.core.InterledgerAddressPrefix;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link AsnInterledgerAddressPrefixCodec}.
 */
public class AsnInterledgerAddressPrefixCodecTest {

  private static final String G = "g";
  private AsnInterledgerAddressPrefixCodec codec;

  @Before
  public void setUp() {
    codec = new AsnInterledgerAddressPrefixCodec();
    codec.setCharString(G);
  }

  @Test
  public void decode() {
    assertThat(codec.decode(), is(InterledgerAddressPrefix.of(G)));
  }

  @Test
  public void encode() {
    codec.encode(InterledgerAddressPrefix.of(G));
    assertThat(codec.getCharString(), is(G));
  }
}