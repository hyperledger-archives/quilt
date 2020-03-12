package org.interledger.quilt.jackson.spsp;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.core.SharedSecret;
import org.interledger.quilt.jackson.sharedsecret.SharedSecretModule;
import org.interledger.quilt.jackson.sharedsecret.SharedSecretSerializer;
import org.interledger.quilt.jackson.spsp.SharedSecretModuleTest.SharedSecretContainer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * Unit tests for {@link SharedSecretSerializer}.
 */
public class SharedSecretSerializerTest {

  private static final SharedSecret SHARED_SECRET = SharedSecret.of(new byte[32]);

  protected ObjectMapper objectMapper;

  @Before
  public void setup() {
    this.objectMapper = new ObjectMapper()
        .registerModule(new SharedSecretModule());
  }


  @Test
  public void shouldSerialize() throws IOException {
    final String actual = objectMapper.writeValueAsString(SHARED_SECRET);
    assertThat(actual).isEqualTo("\"" + SHARED_SECRET.value() + "\"");
  }

  @Test
  public void shouldSerializeInContainer() throws IOException {
    final SharedSecretContainer expectedContainer = ImmutableSharedSecretContainer.builder()
        .sharedSecret(SHARED_SECRET)
        .build();

    final String actualJson = objectMapper.writeValueAsString(expectedContainer);

    assertThat(actualJson).isEqualTo("{\"shared_secret\":\"" + SHARED_SECRET.value() + "\"}");
  }
}
