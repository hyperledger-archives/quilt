package org.interledger.stream.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit test for {@link StreamSharedSecret}.
 */
public class StreamSharedSecretTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void fromBase64Key() {
    Random random = new Random();
    for (int i = 0; i < 100; i++) {
      byte[] key = new byte[32];
      random.nextBytes(key);
      String base64 = Base64.getEncoder().encodeToString(key);
      StreamSharedSecret streamSharedSecret = StreamSharedSecret.of(base64);
      assertThat(streamSharedSecret.value()).isEqualTo(base64);
      assertThat(streamSharedSecret.key()).isEqualTo(key);
    }
  }

  @Test
  public void fromBase64UrlKey() {
    Random random = new Random();
    for (int i = 0; i < 100; i++) {
      byte[] key = new byte[32];
      random.nextBytes(key);
      String base64Url = Base64.getUrlEncoder().encodeToString(key);
      StreamSharedSecret streamSharedSecret = StreamSharedSecret.of(base64Url);
      assertThat(streamSharedSecret.value()).isEqualTo(base64Url);
      assertThat(streamSharedSecret.key()).isEqualTo(key);
    }
  }

  @Test
  public void exceptionIfNotBase64() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("SharedSecret must be base64 encoded");
    StreamSharedSecret.of("!");
  }

  @Test
  public void exceptionIfLessThan32Bytes() {
    final byte[] smallKey = new byte[31];
    String base64 = Base64.getEncoder().encodeToString(smallKey);
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("SharedSecret must be 32 bytes");
    StreamSharedSecret.of(base64);
  }

  @Test
  public void exceptionIfMoreThan32Bytes() {
    final byte[] bigKey = new byte[33];
    String base64 = Base64.getEncoder().encodeToString(bigKey);
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("SharedSecret must be 32 bytes");
    StreamSharedSecret.of(base64);
  }

}
