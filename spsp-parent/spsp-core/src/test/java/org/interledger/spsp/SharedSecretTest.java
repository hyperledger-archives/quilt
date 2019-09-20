package org.interledger.spsp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Base64;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

public class SharedSecretTest {

  private static final byte[] KEY = new byte[32];
  static {
    new Random().nextBytes(new byte[32]);
  }
  private static final String BASE_64_KEY = Base64.getEncoder().encodeToString(KEY);
  private static final String BASE_64_URL_KEY = Base64.getUrlEncoder().encodeToString(KEY);

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void fromBase64Key() {
    SharedSecret sharedSecret = SharedSecret.of(BASE_64_KEY);
    assertThat(sharedSecret.value()).isEqualTo(BASE_64_KEY);
    assertThat(sharedSecret.key()).isEqualTo(KEY);
  }

  @Test
  public void fromBase64UrlKey() {
    SharedSecret sharedSecret = SharedSecret.of(BASE_64_URL_KEY);
    assertThat(sharedSecret.value()).isEqualTo(BASE_64_KEY);
    assertThat(sharedSecret.key()).isEqualTo(KEY);
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

  @Test
  public void jacksonCanDoItsThing() throws JsonProcessingException {
    SharedSecret sharedSecret = SharedSecret.of(BASE_64_KEY);
    ObjectMapper objectMapper = new ObjectMapper();
    String jsonValue = objectMapper.writeValueAsString(sharedSecret);
    SharedSecret fromJackson = objectMapper.readValue(jsonValue, SharedSecret.class);
    assertThat(fromJackson).isEqualTo(sharedSecret);
  }

}