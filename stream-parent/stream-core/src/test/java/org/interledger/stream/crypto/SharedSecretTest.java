package org.interledger.stream.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Base64;
import java.util.Random;

/**
 * Unit test for {@link SharedSecret}.
 */
public class SharedSecretTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void fromBase64Key() {
    Random random = new Random();
    for (int i = 0; i < 100; i++) {
      byte[] key = new byte[32];
      random.nextBytes(key);
      String base64 = Base64.getEncoder().encodeToString(key);
      SharedSecret sharedSecret = SharedSecret.of(base64);
      assertThat(sharedSecret.value()).isEqualTo(base64);
      assertThat(sharedSecret.key()).isEqualTo(key);
    }
  }

  @Test
  public void fromBase64UrlKey() {
    Random random = new Random();
    for (int i = 0; i < 100; i++) {
      byte[] key = new byte[32];
      random.nextBytes(key);
      String base64Url = Base64.getUrlEncoder().encodeToString(key);
      SharedSecret sharedSecret = SharedSecret.of(base64Url);
      assertThat(sharedSecret.value()).isEqualTo(base64Url);
      assertThat(sharedSecret.key()).isEqualTo(key);
    }
  }

  @Test
  public void exceptionIfNotBase64() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("SharedSecret must be base64 encoded");
    SharedSecret.of("!");
  }

  @Test
  public void exceptionIfLessThan32Bytes() {
    final byte[] smallKey = new byte[31];
    String base64 = Base64.getEncoder().encodeToString(smallKey);
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("SharedSecret must be 32 bytes");
    SharedSecret.of(base64);
  }

  @Test
  public void exceptionIfMoreThan32Bytes() {
    final byte[] bigKey = new byte[33];
    String base64 = Base64.getEncoder().encodeToString(bigKey);
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("SharedSecret must be 32 bytes");
    SharedSecret.of(base64);
  }

}
