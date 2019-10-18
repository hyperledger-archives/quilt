package org.interledger.encoding.asn.codecs;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.primitives.UnsignedLong;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.math.BigInteger;

/**
 * Unit tests for {@link AsnUintCodecUL}.
 */
public class AsnUintCodecULTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private AsnUintCodecUL codec;

  @Test
  public void constructWithNullValues() {
    expectedException.expect(RuntimeException.class);
    new AsnUintCodecUL(null);
  }

  @Test
  public void decode() {
    codec = new AsnUintCodecUL();
    codec.setBytes(new byte[]{1});
    assertThat(codec.decode()).isEqualTo(UnsignedLong.ONE);
  }

  @Test
  public void decodeValueTooLarge(){
    codec = new AsnUintCodecUL(UnsignedLong.ONE);
    byte[] bytes = new BigInteger("18446744073709551616").toByteArray();
    codec.setBytes(bytes);
    assertThat(codec.decode()).isEqualTo(UnsignedLong.ONE);
  }
  @Test
  public void encode() {
    codec = new AsnUintCodecUL();
    codec.encode(UnsignedLong.ONE);
    assertThat(codec.getBytes()).isEqualTo(new byte[] {1});
  }
}
