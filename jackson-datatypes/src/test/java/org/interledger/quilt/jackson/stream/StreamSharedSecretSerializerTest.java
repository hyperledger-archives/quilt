package org.interledger.quilt.jackson.stream;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.quilt.jackson.stream.StreamSharedSecretModuleTest.StreamSharedSecretContainer;
import org.interledger.stream.crypto.StreamSharedSecret;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * Unit tests for {@link StreamSharedSecretSerializer}.
 */
public class StreamSharedSecretSerializerTest {

  private static final StreamSharedSecret SHARED_SECRET = StreamSharedSecret.of(new byte[32]);

  protected ObjectMapper objectMapper;

  @Before
  public void setup() {
    this.objectMapper = new ObjectMapper()
      .registerModule(new StreamSharedSecretModule());
  }


  @Test
  public void shouldSerialize() throws IOException {
    final String actual = objectMapper.writeValueAsString(SHARED_SECRET);
    assertThat(actual).isEqualTo("\"" + SHARED_SECRET.value() + "\"");
  }

  @Test
  public void shouldSerializeInContainer() throws IOException {
    final StreamSharedSecretContainer expectedContainer = ImmutableStreamSharedSecretContainer.builder()
      .streamSharedSecret(SHARED_SECRET)
      .build();

    final String actualJson = objectMapper.writeValueAsString(expectedContainer);

    assertThat(actualJson).isEqualTo("{\"shared_secret\":\"" + SHARED_SECRET.value() + "\"}");
  }
}
